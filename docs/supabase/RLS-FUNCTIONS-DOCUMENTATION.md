# Supabase RLS & Functions Documentation

## Table of Contents
1. [Overview](#overview)
2. [User Roles](#user-roles)
3. [Helper Functions](#helper-functions)
4. [RLS Policies by Table](#rls-policies-by-table)
5. [Database Functions](#database-functions)
6. [Triggers](#triggers)

---

## Overview

Dự án FoodCouriers sử dụng Supabase với **Row Level Security (RLS)** để bảo mật dữ liệu. Mỗi bảng đều có RLS enabled và các policy kiểm soát quyền truy cập dựa trên role của user.

### Two-Layer Authentication System

| Layer | Source | Values |
|-------|--------|--------|
| Supabase Auth | JWT Token / User Metadata | `super_admin`, `admin`, `shipper`, `customer` |
| Database | `public.users.role` column | `admin`, `shipper`, `staff`, `customer` |

**Lưu ý:** Khuyến nghị sử dụng database role để check quyền trong RLS policies thay vì JWT role để tránh mismatch.

---

## User Roles

### Database Roles (public.users.role)

| Role | Description | Permissions |
|------|-------------|--------------|
| `admin` | Quản trị viên | Full CRUD trên tất cả bảng, quản lý users, orders, restaurants |
| `shipper` | Tài xế | Xem đơn hàng được gán, cập nhật trạng thái giao hàng |
| `staff` | Nhân viên nhà hàng | Quản lý menu, category, đơn hàng của restaurant |
| `customer` | Khách hàng | Xem đơn hàng của mình, cập nhật profile |

### Role Hierarchy

```
admin (full access)
  └── staff (restaurant-specific)
       └── shipper (delivery-specific)
            └── customer (own data only)
```

---

## Helper Functions

### Schema: `private` (SECURITY DEFINER)

Các function trong schema `private` được tạo với `SECURITY DEFINER` để bypass RLS khi kiểm tra quyền.

### 1. get_current_user_id()

```sql
-- Lấy user_id từ auth.users dựa trên JWT
CREATE OR REPLACE FUNCTION private.get_current_user_id()
RETURNS UUID
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT id FROM public.users WHERE auth_id = auth.uid();
$$;
```

**Purpose:** Lấy ID của user đang logged in từ database

---

### 2. is_current_user_shipper()

```sql
-- Kiểm tra user hiện tại có role shipper trong database không
CREATE OR REPLACE FUNCTION private.is_current_user_shipper()
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.users 
        WHERE auth_id = auth.uid() AND role = 'shipper'
    );
$$;
```

**Purpose:** Kiểm tra user có phải là shipper không

---

### 3. is_current_user_admin()

```sql
-- Kiểm tra user hiện tại có role admin trong database không
CREATE OR REPLACE FUNCTION private.is_current_user_admin()
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.users 
        WHERE auth_id = auth.uid() AND role = 'admin'
    );
$$;
```

**Purpose:** Kiểm tra user có phải là admin không

---

### Schema: `public`

### 4. is_admin()

```sql
-- Function công khai để sử dụng trong RLS policies
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.users 
        WHERE auth_id = auth.uid() 
        AND role = 'admin'
    );
$$;

GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated, service_role;
```

**Purpose:** Dùng trong RLS policies để kiểm tra quyền admin

---

## RLS Policies by Table

### 1. users

```sql
-- User có thể xem profile của chính mình
CREATE POLICY "users_select_own" ON users
    FOR SELECT USING (auth_id = auth.uid());

-- Admin có thể xem tất cả users
CREATE POLICY "users_select_admin" ON users
    FOR SELECT USING (private.is_current_user_admin());

-- User có thể cập nhật profile của chính mình
CREATE POLICY "users_update_own" ON users
    FOR UPDATE USING (auth_id = auth.uid());

-- Admin có thể cập nhật bất kỳ user nào
CREATE POLICY "users_update_admin" ON users
    FOR UPDATE USING (private.is_current_user_admin());
```

**Tables with RLS:** users, user_addresses

---

### 2. categories

```sql
-- Tất cả user đã auth có thể xem categories active
CREATE POLICY "categories_select" ON public.categories
    FOR SELECT USING (is_active = true);

-- Chỉ admin mới có quyền CRUD
CREATE POLICY "categories_admin_manage" ON public.categories
    FOR ALL 
    USING (is_admin())
    WITH CHECK (is_admin());
```

---

### 3. restaurants

```sql
-- Tất cả user có thể xem restaurants active
CREATE POLICY "restaurants_select_all" ON restaurants
    FOR SELECT USING (is_active = true);

-- Chỉ admin mới có quyền CRUD
CREATE POLICY "restaurants_manage" ON restaurants
    FOR ALL USING (private.is_current_user_admin())
    WITH CHECK (private.is_current_user_admin());
```

---

### 4. menu_items

```sql
-- Tất cả user có thể xem menu items available
CREATE POLICY "menu_items_select" ON menu_items
    FOR SELECT USING (
        is_available = true AND
        restaurant_id IN (SELECT id FROM restaurants WHERE is_active = true)
    );

-- Admin và staff có quyền quản lý
CREATE POLICY "menu_items_manage" ON menu_items
    FOR ALL USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() 
        AND role IN ('admin', 'staff'))
    )
    WITH CHECK (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() 
        AND role IN ('admin', 'staff'))
    );
```

---

### 5. orders

```sql
-- Customer: Xem đơn hàng của chính mình
CREATE POLICY "orders_select_own" ON orders
    FOR SELECT USING (user_id = private.get_current_user_id());

-- Admin: Xem tất cả đơn hàng
CREATE POLICY "orders_select_all" ON orders
    FOR SELECT USING (private.is_current_user_admin());

-- Admin: Cập nhật đơn hàng
CREATE POLICY "orders_update_all" ON orders
    FOR UPDATE USING (private.is_current_user_admin());

-- Shipper: Xem đơn được gán + đơn available
CREATE POLICY "orders_select_shipper" ON public.orders
    FOR SELECT TO authenticated
    USING (
        private.is_current_user_shipper()
        AND (
            orders.shipper_id = private.get_current_user_id()
            OR (
                orders.shipper_id IS NULL 
                AND orders.status IN ('confirmed', 'preparing', 'ready_for_pickup')
                AND orders.delivery_status IN ('unassigned', 'searching')
            )
        )
    );
```

---

### 6. shippers

```sql
-- Shipper xem profile của chính mình
CREATE POLICY "shippers_select_own" ON public.shippers
    FOR SELECT USING (user_id = private.get_current_user_id());

-- Admin xem tất cả shippers
CREATE POLICY "shippers_select_all" ON public.shippers
    FOR SELECT USING (private.is_current_user_admin());
```

---

### 7. order_items

```sql
-- Customer, Admin, Shipper đều có thể xem qua orders policy
-- Shipper có thể xem items của đơn hàng họ nhận
CREATE POLICY "order_items_select_shipper" ON public.order_items
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.id = order_items.order_id
        )
    );
```

---

### 8. order_status_logs

```sql
-- Tương tự order_items
CREATE POLICY "order_status_logs_select_shipper" ON public.order_status_logs
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.id = order_status_logs.order_id
        )
    );
```

---

### 9. promotions

```sql
-- Tất cả user có thể xem promotions active
CREATE POLICY "promotions_select" ON promotions
    FOR SELECT USING (is_active = true);

-- Chỉ admin quản lý
CREATE POLICY "promotions_manage" ON promotions
    FOR ALL USING (private.is_current_user_admin())
    WITH CHECK (private.is_current_user_admin());
```

---

### 10. notifications

```sql
-- User chỉ xem được notification của mình
CREATE POLICY "notifications_select_own" ON notifications
    FOR SELECT USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );

CREATE POLICY "notifications_update_own" ON notifications
    FOR UPDATE USING (
        user_id IN (SELECT id FROM users WHERE auth_id = auth.uid())
    );
```

---

## Database Functions

### 1. accept_order(p_order_id UUID, p_shipper_user_id UUID)

```sql
-- Shipper nhận đơn hàng
CREATE OR REPLACE FUNCTION public.accept_order(p_order_id UUID, p_shipper_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_order RECORD;
    v_shipper_exists BOOLEAN;
BEGIN
    -- Kiểm tra shipper tồn tại và active
    SELECT EXISTS(SELECT 1 FROM shippers WHERE user_id = p_shipper_user_id AND is_active = true) 
    INTO v_shipper_exists;
    IF NOT v_shipper_exists THEN
        RETURN jsonb_build_object('success', false, 'message', 'Tài khoản Shipper không hợp lệ.');
    END IF;

    -- Lock dòng để tránh race condition
    SELECT status, delivery_status, shipper_id INTO v_order 
    FROM orders WHERE id = p_order_id FOR UPDATE;

    -- Kiểm tra đơn đã có shipper chưa
    IF v_order.shipper_id IS NOT NULL THEN
        RETURN jsonb_build_object('success', false, 'message', 'Đơn hàng đã có người khác nhận.');
    END IF;

    -- Kiểm tra trạng thái đơn có thể nhận
    IF v_order.status NOT IN ('confirmed', 'preparing', 'ready_for_pickup') THEN
        RETURN jsonb_build_object('success', false, 'message', 'Đơn hàng không trong trạng thái có thể tiếp nhận.');
    END IF;

    -- Cập nhật shipper cho đơn
    UPDATE orders SET 
        shipper_id = p_shipper_user_id, 
        delivery_status = 'assigned',
        updated_at = NOW()
    WHERE id = p_order_id;

    -- Cập nhật trạng thái shipper
    UPDATE shippers SET delivery_status = 'busy' WHERE user_id = p_shipper_user_id;

    -- Log thay đổi
    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_order.status, v_order.status, p_shipper_user_id, 'Shipper accepted order');

    RETURN jsonb_build_object('success', true, 'message', 'Nhận đơn thành công.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

**Use Case:** Shipper nhận đơn từ màn hình "Đơn hàng mới"

---

### 2. pickup_order(p_order_id UUID, p_shipper_user_id UUID)

```sql
-- Shipper xác nhận đã lấy hàng
CREATE OR REPLACE FUNCTION public.pickup_order(p_order_id UUID, p_shipper_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_order RECORD;
BEGIN
    SELECT status, delivery_status, shipper_id INTO v_order 
    FROM orders WHERE id = p_order_id FOR UPDATE;

    -- Kiểm tra quyền
    IF v_order.shipper_id != p_shipper_user_id THEN
        RETURN jsonb_build_object('success', false, 'message', 'Bạn không có quyền xử lý đơn này.');
    END IF;

    -- Cập nhật trạng thái
    UPDATE orders SET 
        status = 'delivering',
        delivery_status = 'picked_up',
        updated_at = NOW()
    WHERE id = p_order_id;

    -- Log
    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_order.status, 'delivering', p_shipper_user_id, 'Shipper picked up items');

    RETURN jsonb_build_object('success', true, 'message', 'Xác nhận lấy hàng thành công.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

**Use Case:** Shipper bấm "Đã lấy hàng" khi nhận món từ nhà hàng

---

### 3. complete_order(p_order_id UUID, p_shipper_user_id UUID)

```sql
-- Shipper hoàn thành giao hàng
CREATE OR REPLACE FUNCTION public.complete_order(p_order_id UUID, p_shipper_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_order RECORD;
BEGIN
    SELECT status, delivery_status, shipper_id, total INTO v_order 
    FROM orders WHERE id = p_order_id FOR UPDATE;

    IF v_order.shipper_id != p_shipper_user_id THEN
        RETURN jsonb_build_object('success', false, 'message', 'Bạn không có quyền xử lý đơn này.');
    END IF;

    -- Cập nhật đơn hàng hoàn thành
    UPDATE orders SET 
        status = 'delivered',
        delivery_status = 'completed',
        payment_status = 'paid',
        updated_at = NOW()
    WHERE id = p_order_id;

    -- Cập nhật shipper stats
    UPDATE shippers SET 
        delivery_status = 'available',
        total_delivered = total_delivered + 1,
        total_revenue = total_revenue + v_order.total
    WHERE user_id = p_shipper_user_id;

    -- Log
    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_order.status, 'delivered', p_shipper_user_id, 'Order delivered successfully');

    RETURN jsonb_build_object('success', true, 'message', 'Hoàn thành đơn hàng.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

**Use Case:** Shipper bấm "Hoàn thành" khi giao hàng thành công

---

### 4. assign_shipper(p_order_id UUID, p_shipper_id UUID)

```sql
-- Admin gán shipper cho đơn
CREATE OR REPLACE FUNCTION public.assign_shipper(p_order_id UUID, p_shipper_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_order RECORD;
BEGIN
    SELECT * INTO v_order FROM orders WHERE id = p_order_id FOR UPDATE;

    IF v_order.status IN ('delivered', 'cancelled') THEN
        RETURN jsonb_build_object('success', false, 'message', 'Không thể gán shipper cho đơn đã hoàn thành.');
    END IF;

    IF v_order.shipper_id IS NOT NULL THEN
        RETURN jsonb_build_object('success', false, 'message', 'Đơn hàng đã có shipper.');
    END IF;

    UPDATE orders SET 
        shipper_id = p_shipper_id,
        delivery_status = 'assigned',
        updated_at = NOW()
    WHERE id = p_order_id;

    UPDATE shippers SET delivery_status = 'busy' WHERE user_id = p_shipper_id;

    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_order.status, v_order.status, p_shipper_id, 'Admin assigned shipper');

    RETURN jsonb_build_object('success', true, 'message', 'Gán shipper thành công.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

**Use Case:** Admin gán shipper thủ công từ màn hình quản lý đơn hàng

---

## Triggers

### 1. release_shipper_on_order_end()

```sql
-- Trigger giải phóng shipper khi đơn hàng kết thúc
CREATE OR REPLACE FUNCTION public.release_shipper_on_order_end()
RETURNS TRIGGER AS $$
BEGIN
    -- Khi đơn chuyển sang delivered hoặc cancelled, giải phóng shipper
    IF (NEW.status IN ('delivered', 'cancelled')) AND (NEW.shipper_id IS NOT NULL) THEN
        UPDATE public.shippers 
        SET delivery_status = 'available'
        WHERE user_id = NEW.shipper_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Tạo trigger
DROP TRIGGER IF EXISTS tr_release_shipper_on_order_end ON public.orders;
CREATE TRIGGER tr_release_shipper_on_order_end
    AFTER UPDATE OF status ON public.orders
    FOR EACH ROW
    WHEN (NEW.status IN ('delivered', 'cancelled'))
    EXECUTE FUNCTION public.release_shipper_on_order_end();
```

**Purpose:** Tự động set shipper về available khi đơn hàng hoàn thành hoặc bị hủy

---

### 2. update_updated_at_column()

```sql
-- Trigger tự động cập nhật updated_at khi row thay đổi
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Áp dụng cho các bảng cần
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at 
    BEFORE UPDATE ON orders FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_shippers_updated_at 
    BEFORE UPDATE ON shippers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

---

## Permissions Grant

```sql
-- Cấp quyền cho authenticated users
GRANT SELECT ON public.orders TO authenticated;
GRANT SELECT ON public.shippers TO authenticated;

-- Cấp quyền execute cho functions
GRANT EXECUTE ON FUNCTION public.accept_order(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.pickup_order(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.complete_order(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.assign_shipper(UUID, UUID) TO authenticated;

GRANT EXECUTE ON FUNCTION private.get_current_user_id() TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION private.is_current_user_shipper() TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION private.is_current_user_admin() TO authenticated, service_role;
```

---

## Best Practices

1. **Sử dụng database role thay vì JWT role** trong RLS policies để tránh mismatch
2. **Dùng schema private** cho các helper function với `SECURITY DEFINER` để bypass RLS khi cần
3. **Sử dụng FOR UPDATE** trong các function thay đổi data để tránh race conditions
4. **Luôn log** các thay đổi quan trọng vào `order_status_logs`
5. **Grant quyền tối thiểu** - chỉ grant những gì cần thiết

---

## Troubleshooting

### Lỗi "new row violates row-level security policy"

**Nguyên nhân:** Policy không khớp với role của user

**Giải pháp:** 
1. Kiểm tra function `is_admin()` có đang check đúng role không
2. Đảm bảo user có record trong bảng `users` với role đúng
3. Kiểm tra JWT token có đúng role không

### Lỗi "permission denied"

**Nguyên nhân:** Chưa grant quyền cho user/function

**Giải pháp:**
1. Kiểm tra RLS đã enabled chưa
2. Verify đã grant quyền cho function chưa
3. Kiểm tra user đã authenticate chưa