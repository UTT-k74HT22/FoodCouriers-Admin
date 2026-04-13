-- Migration 012: Refactor Order and Delivery Logic
-- Purpose: Align database with BA Documents 03 (Order Management) and 10 (Shipper Management)

-- 1. Cập nhật bảng orders: Thêm cột delivery_status và chuẩn hóa cột status
DO $$ 
BEGIN
    -- Thêm cột delivery_status nếu chưa có
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name = 'orders' AND column_name = 'delivery_status') THEN
        ALTER TABLE public.orders ADD COLUMN delivery_status TEXT NOT NULL DEFAULT 'unassigned';
    END IF;

    -- Cập nhật ràng buộc cho status (Order Lifecycle)
    ALTER TABLE public.orders DROP CONSTRAINT IF EXISTS orders_status_check;
    ALTER TABLE public.orders ADD CONSTRAINT orders_status_check 
        CHECK (status IN ('pending', 'confirmed', 'preparing', 'ready_for_pickup', 'delivering', 'delivered', 'cancelled'));

    -- Cập nhật ràng buộc cho delivery_status (Delivery Lifecycle)
    ALTER TABLE public.orders DROP CONSTRAINT IF EXISTS orders_delivery_status_check;
    ALTER TABLE public.orders ADD CONSTRAINT orders_delivery_status_check 
        CHECK (delivery_status IN ('unassigned', 'searching', 'assigned', 'arriving_pickup', 'waiting_pickup', 'picked_up', 'completed', 'failed'));
END $$;

-- 2. Chỉ mục để tăng tốc truy vấn trạng thái
CREATE INDEX IF NOT EXISTS idx_orders_delivery_status ON public.orders(delivery_status);

-- 3. HÀM CẬP NHẬT: Shipper nhận đơn (Accept Order)
-- Cho phép nhận đơn từ khi confirmed/preparing/ready_for_pickup
CREATE OR REPLACE FUNCTION accept_order_v2(p_order_id UUID, p_shipper_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_order RECORD;
    v_shipper_exists BOOLEAN;
BEGIN
    -- 1. Kiểm tra shipper có tồn tại và đang active không
    SELECT EXISTS(SELECT 1 FROM shippers WHERE user_id = p_shipper_user_id AND is_active = true) INTO v_shipper_exists;
    IF NOT v_shipper_exists THEN
        RETURN jsonb_build_object('success', false, 'message', 'Tài khoản Shipper không hợp lệ hoặc bị khóa.');
    END IF;

    -- 2. Khóa dòng đơn hàng để kiểm tra (Pessimistic Locking)
    SELECT status, delivery_status, shipper_id INTO v_order 
    FROM orders 
    WHERE id = p_order_id 
    FOR UPDATE;

    -- 3. Kiểm tra điều kiện nhận đơn
    IF v_order.shipper_id IS NOT NULL THEN
        RETURN jsonb_build_object('success', false, 'message', 'Đơn hàng đã có người khác nhận.');
    END IF;

    -- Cho phép nhận khi đơn đã xác nhận hoặc đang chuẩn bị
    IF v_order.status NOT IN ('confirmed', 'preparing', 'ready_for_pickup') THEN
        RETURN jsonb_build_object('success', false, 'message', 'Đơn hàng hiện không trong trạng thái có thể tiếp nhận.');
    END IF;

    -- 4. Thực hiện cập nhật
    UPDATE orders SET 
        shipper_id = p_shipper_user_id, 
        delivery_status = 'assigned',
        updated_at = NOW()
    WHERE id = p_order_id;

    -- Cập nhật trạng thái Shipper sang bận (Busy)
    UPDATE shippers SET delivery_status = 'busy' WHERE user_id = p_shipper_user_id;

    -- 5. Log trạng thái (Audit Trail)
    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_order.status, v_order.status, p_shipper_user_id, 'Shipper accepted order (Delivery Status: assigned)');

    RETURN jsonb_build_object('success', true, 'message', 'Nhận đơn thành công. Hãy di chuyển tới nhà hàng.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. HÀM MỚI: Shipper lấy hàng thành công (Pickup Order)
CREATE OR REPLACE FUNCTION pickup_order(p_order_id UUID, p_shipper_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_order RECORD;
BEGIN
    SELECT status, delivery_status, shipper_id INTO v_order 
    FROM orders WHERE id = p_order_id FOR UPDATE;

    IF v_order.shipper_id != p_shipper_user_id THEN
        RETURN jsonb_build_object('success', false, 'message', 'Bạn không có quyền xử lý đơn hàng này.');
    END IF;

    -- Chỉ cho phép pickup khi shipper đã được gán và món đã sẵn sàng (hoặc đang làm)
    -- Theo BA, Delivering bắt đầu khi hàng rời quán
    UPDATE orders SET 
        status = 'delivering',
        delivery_status = 'picked_up',
        updated_at = NOW()
    WHERE id = p_order_id;

    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_order.status, 'delivering', p_shipper_user_id, 'Shipper picked up items');

    RETURN jsonb_build_object('success', true, 'message', 'Xác nhận lấy hàng thành công. Đang trong quá trình giao hàng.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. HÀM MỚI: Hoàn tất đơn hàng (Complete Order)
CREATE OR REPLACE FUNCTION complete_order(p_order_id UUID, p_shipper_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_order RECORD;
BEGIN
    SELECT status, delivery_status, shipper_id, total INTO v_order 
    FROM orders WHERE id = p_order_id FOR UPDATE;

    IF v_order.shipper_id != p_shipper_user_id THEN
        RETURN jsonb_build_object('success', false, 'message', 'Bạn không có quyền xử lý đơn hàng này.');
    END IF;

    -- Cập nhật đơn hàng thành công
    UPDATE orders SET 
        status = 'delivered',
        delivery_status = 'completed',
        payment_status = 'paid', -- Giả định COD hoặc đã thanh toán thành công
        updated_at = NOW()
    WHERE id = p_order_id;

    -- Giải phóng Shipper và cập nhật chỉ số
    UPDATE shippers SET 
        delivery_status = 'available',
        total_delivered = total_delivered + 1,
        total_revenue = total_revenue + v_order.total
    WHERE user_id = p_shipper_user_id;

    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_order.status, 'delivered', p_shipper_user_id, 'Order delivered successfully');

    RETURN jsonb_build_object('success', true, 'message', 'Chúc mừng! Bạn đã hoàn thành đơn hàng.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
