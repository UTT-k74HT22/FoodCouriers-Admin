-- =====================================================
-- FoodCouriers Admin - Database Schema Migration
-- Version: 2.0 (Consolidated)
-- Date: 2026-04-15
-- Description: Complete schema after refactoring order management
-- =====================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =====================================================
-- TABLE: users (v2 - with shipper role)
-- =====================================================
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    auth_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    phone TEXT,
    email TEXT NOT NULL,
    avatar_url TEXT,
    role TEXT NOT NULL CHECK (role IN ('customer', 'shipper', 'staff', 'admin')) DEFAULT 'customer',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_auth_id ON public.users(auth_id);
CREATE INDEX IF NOT EXISTS idx_users_role ON public.users(role);

-- =====================================================
-- TABLE: shippers
-- =====================================================
CREATE TABLE IF NOT EXISTS public.shippers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    restaurant_id UUID REFERENCES public.restaurants(id) ON DELETE SET NULL,
    license_plate TEXT,
    vehicle_type TEXT,
    delivery_status TEXT NOT NULL DEFAULT 'available' CHECK (delivery_status IN ('available', 'busy', 'offline')),
    is_available BOOLEAN NOT NULL DEFAULT true,
    is_active BOOLEAN NOT NULL DEFAULT true,
    total_delivered INTEGER DEFAULT 0,
    total_revenue BIGINT DEFAULT 0,
    rating DECIMAL(3,2),
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

CREATE INDEX IF NOT EXISTS idx_shippers_user_id ON public.shippers(user_id);
CREATE INDEX IF NOT EXISTS idx_shippers_delivery_status ON public.shippers(delivery_status);
CREATE INDEX IF NOT EXISTS idx_shippers_is_available ON public.shippers(is_available);

-- Enable RLS for shippers
ALTER TABLE public.shippers ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- TABLE: orders (v2 - with shipper and delivery status)
-- =====================================================
CREATE TABLE IF NOT EXISTS public.orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_code TEXT NOT NULL UNIQUE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    restaurant_id UUID REFERENCES restaurants(id) ON DELETE SET NULL,
    shipper_id UUID REFERENCES users(id) ON DELETE SET NULL,
    delivery_address TEXT NOT NULL,
    delivery_latitude DOUBLE PRECISION,
    delivery_longitude DOUBLE PRECISION,
    note TEXT,
    subtotal INTEGER NOT NULL,
    delivery_fee INTEGER NOT NULL DEFAULT 0,
    discount INTEGER NOT NULL DEFAULT 0,
    total INTEGER NOT NULL,
    payment_method TEXT NOT NULL CHECK (payment_method IN ('cod', 'online')) DEFAULT 'cod',
    payment_status TEXT NOT NULL CHECK (payment_status IN ('pending', 'paid', 'failed', 'refunded')) DEFAULT 'pending',
    status TEXT NOT NULL CHECK (status IN ('pending', 'confirmed', 'preparing', 'ready_for_pickup', 'delivering', 'delivered', 'cancelled')) DEFAULT 'pending',
    delivery_status TEXT NOT NULL DEFAULT 'unassigned' CHECK (delivery_status IN ('unassigned', 'searching', 'assigned', 'arriving_pickup', 'waiting_pickup', 'picked_up', 'completed', 'failed')),
    cancelled_reason TEXT,
    promotion_id UUID REFERENCES promotions(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_user ON public.orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_restaurant ON public.orders(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_orders_shipper_id ON public.orders(shipper_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON public.orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_delivery_status ON public.orders(delivery_status);

-- =====================================================
-- Update trigger for updated_at
-- =====================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_shippers_updated_at ON shippers;
CREATE TRIGGER update_shippers_updated_at
    BEFORE UPDATE ON shippers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- RLS Policies - Consolidated
-- =====================================================
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shippers ENABLE ROW LEVEL SECURITY;

-- Helper functions for RLS
CREATE SCHEMA IF NOT EXISTS private;

CREATE OR REPLACE FUNCTION private.get_current_user_id()
RETURNS UUID
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT id FROM public.users WHERE auth_id = auth.uid();
$$;

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

GRANT EXECUTE ON FUNCTION private.get_current_user_id() TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION private.is_current_user_shipper() TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION private.is_current_user_admin() TO authenticated, service_role;

-- Users policies
DROP POLICY IF EXISTS "users_select_own" ON public.users;
DROP POLICY IF EXISTS "users_select_admin" ON public.users;
CREATE POLICY "users_select_own" ON users
    FOR SELECT USING (auth_id = auth.uid());
CREATE POLICY "users_select_admin" ON users
    FOR SELECT USING (private.is_current_user_admin());

-- Orders policies (Customer)
DROP POLICY IF EXISTS "orders_select_own" ON public.orders;
CREATE POLICY "orders_select_own" ON orders
    FOR SELECT USING (user_id = private.get_current_user_id());

-- Orders policies (Admin)
DROP POLICY IF EXISTS "orders_select_all" ON public.orders;
CREATE POLICY "orders_select_all" ON orders
    FOR SELECT USING (private.is_current_user_admin());
CREATE POLICY "orders_update_all" ON orders
    FOR UPDATE USING (private.is_current_user_admin());

-- Orders policies (Shipper)
DROP POLICY IF EXISTS "orders_select_shipper" ON public.orders;
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

-- Shippers policies
DROP POLICY IF EXISTS "shippers_select_own" ON public.shippers;
CREATE POLICY "shippers_select_own" ON public.shippers
    FOR SELECT USING (user_id = private.get_current_user_id());
CREATE POLICY "shippers_select_all" ON public.shippers
    FOR SELECT USING (private.is_current_user_admin());

-- =====================================================
-- Order Management Functions
-- =====================================================

-- 1. Accept Order (Shipper nhận đơn)
CREATE OR REPLACE FUNCTION public.accept_order(p_order_id UUID, p_shipper_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_order RECORD;
    v_shipper_exists BOOLEAN;
BEGIN
    SELECT EXISTS(SELECT 1 FROM shippers WHERE user_id = p_shipper_user_id AND is_active = true) INTO v_shipper_exists;
    IF NOT v_shipper_exists THEN
        RETURN jsonb_build_object('success', false, 'message', 'Tài khoản Shipper không hợp lệ hoặc bị khóa.');
    END IF;

    SELECT status, delivery_status, shipper_id INTO v_order 
    FROM orders WHERE id = p_order_id FOR UPDATE;

    IF v_order.shipper_id IS NOT NULL THEN
        RETURN jsonb_build_object('success', false, 'message', 'Đơn hàng đã có người khác nhận.');
    END IF;

    IF v_order.status NOT IN ('confirmed', 'preparing', 'ready_for_pickup') THEN
        RETURN jsonb_build_object('success', false, 'message', 'Đơn hàng hiện không trong trạng thái có thể tiếp nhận.');
    END IF;

    UPDATE orders SET 
        shipper_id = p_shipper_user_id, 
        delivery_status = 'assigned',
        updated_at = NOW()
    WHERE id = p_order_id;

    UPDATE shippers SET delivery_status = 'busy' WHERE user_id = p_shipper_user_id;

    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_order.status, v_order.status, p_shipper_user_id, 'Shipper accepted order');

    RETURN jsonb_build_object('success', true, 'message', 'Nhận đơn thành công.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Pickup Order (Shipper lấy hàng)
CREATE OR REPLACE FUNCTION public.pickup_order(p_order_id UUID, p_shipper_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_order RECORD;
BEGIN
    SELECT status, delivery_status, shipper_id INTO v_order 
    FROM orders WHERE id = p_order_id FOR UPDATE;

    IF v_order.shipper_id != p_shipper_user_id THEN
        RETURN jsonb_build_object('success', false, 'message', 'Bạn không có quyền xử lý đơn hàng này.');
    END IF;

    UPDATE orders SET 
        status = 'delivering',
        delivery_status = 'picked_up',
        updated_at = NOW()
    WHERE id = p_order_id;

    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_order.status, 'delivering', p_shipper_user_id, 'Shipper picked up items');

    RETURN jsonb_build_object('success', true, 'message', 'Xác nhận lấy hàng thành công.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Complete Order (Shipper hoàn thành giao hàng)
CREATE OR REPLACE FUNCTION public.complete_order(p_order_id UUID, p_shipper_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_order RECORD;
BEGIN
    SELECT status, delivery_status, shipper_id, total INTO v_order 
    FROM orders WHERE id = p_order_id FOR UPDATE;

    IF v_order.shipper_id != p_shipper_user_id THEN
        RETURN jsonb_build_object('success', false, 'message', 'Bạn không có quyền xử lý đơn hàng này.');
    END IF;

    UPDATE orders SET 
        status = 'delivered',
        delivery_status = 'completed',
        payment_status = 'paid',
        updated_at = NOW()
    WHERE id = p_order_id;

    UPDATE shippers SET 
        delivery_status = 'available',
        total_delivered = total_delivered + 1,
        total_revenue = total_revenue + v_order.total
    WHERE user_id = p_shipper_user_id;

    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_order.status, 'delivered', p_shipper_user_id, 'Order delivered successfully');

    RETURN jsonb_build_object('success', true, 'message', 'Hoàn thành đơn hàng.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Assign Shipper (Admin gán shipper cho đơn)
CREATE OR REPLACE FUNCTION public.assign_shipper(p_order_id UUID, p_shipper_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_order RECORD;
BEGIN
    SELECT * INTO v_order FROM orders WHERE id = p_order_id FOR UPDATE;

    IF v_order.status IN ('delivered', 'cancelled') THEN
        RETURN jsonb_build_object('success', false, 'message', 'Không thể gán shipper cho đơn hàng đã hoàn thành hoặc đã hủy.');
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

-- =====================================================
-- Auto Release Shipper Trigger
-- =====================================================
CREATE OR REPLACE FUNCTION public.release_shipper_on_order_end()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.status IN ('delivered', 'cancelled')) AND (NEW.shipper_id IS NOT NULL) THEN
        UPDATE public.shippers 
        SET delivery_status = 'available'
        WHERE user_id = NEW.shipper_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS tr_release_shipper_on_order_end ON public.orders;
CREATE TRIGGER tr_release_shipper_on_order_end
    AFTER UPDATE OF status ON public.orders
    FOR EACH ROW
    WHEN (NEW.status IN ('delivered', 'cancelled'))
    EXECUTE FUNCTION public.release_shipper_on_order_end();

-- =====================================================
-- Grant permissions
-- =====================================================
GRANT SELECT ON public.orders TO authenticated;
GRANT SELECT ON public.shippers TO authenticated;
GRANT EXECUTE ON FUNCTION public.accept_order(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.pickup_order(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.complete_order(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.assign_shipper(UUID, UUID) TO authenticated;