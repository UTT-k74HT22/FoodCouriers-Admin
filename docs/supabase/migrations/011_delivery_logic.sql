-- Migration 011: Delivery Management Core Logic
-- 1. Thêm shipper_id vào bảng orders (Liên kết với users.id)
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS shipper_id UUID REFERENCES public.users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_orders_shipper_id ON public.orders(shipper_id);

-- 2. Cập nhật các trạng thái đơn hàng hợp lệ
ALTER TABLE public.orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE public.orders ADD CONSTRAINT orders_status_check 
    CHECK (status IN ('pending', 'confirmed', 'preparing', 'ready_for_pickup', 'delivering', 'delivered', 'cancelled'));

-- 3. Thêm trạng thái hoạt động chi tiết cho Shipper
ALTER TABLE public.shippers ADD COLUMN IF NOT EXISTS delivery_status TEXT 
    NOT NULL DEFAULT 'available' 
    CHECK (delivery_status IN ('available', 'busy', 'offline'));

-- 4. HÀM QUAN TRỌNG: Chấp nhận đơn hàng (Tránh tranh chấp)
CREATE OR REPLACE FUNCTION accept_order(p_order_id UUID, p_shipper_user_id UUID)
RETURNS JSON AS $$
DECLARE
    v_order_status TEXT;
    v_current_shipper UUID;
BEGIN
    -- Khóa dòng đơn hàng để kiểm tra (Pessimistic Locking)
    SELECT status, shipper_id INTO v_order_status, v_current_shipper 
    FROM orders 
    WHERE id = p_order_id 
    FOR UPDATE;

    -- Kiểm tra điều kiện: Đơn phải ở trạng thái 'ready_for_pickup' và chưa có ai nhận
    IF v_order_status != 'ready_for_pickup' THEN
        RETURN json_build_object('success', false, 'message', 'Đơn hàng không còn ở trạng thái chờ shipper.');
    END IF;

    IF v_current_shipper IS NOT NULL THEN
        RETURN json_build_object('success', false, 'message', 'Đơn hàng đã có người khác nhận.');
    END IF;

    -- Thực hiện cập nhật Đơn hàng
    UPDATE orders SET 
        shipper_id = p_shipper_user_id, 
        status = 'delivering',
        updated_at = NOW()
    WHERE id = p_order_id;

    -- Cập nhật trạng thái Shipper sang bận (Busy)
    UPDATE shippers SET delivery_status = 'busy' WHERE user_id = p_shipper_user_id;

    RETURN json_build_object('success', true, 'message', 'Nhận đơn thành công.');
END;
$$ LANGUAGE plpgsql;
