-- Migration 016: Auto Release Shipper Trigger
-- Purpose: Automatically set shipper status back to 'available' when an order is completed or cancelled

CREATE OR REPLACE FUNCTION public.release_shipper_on_order_end()
RETURNS TRIGGER AS $$
BEGIN
    -- Nếu trạng thái đơn hàng chuyển sang 'delivered' hoặc 'cancelled'
    -- Giải phóng Shipper (nếu có) về trạng thái 'available'
    IF (NEW.status IN ('delivered', 'cancelled')) AND (NEW.shipper_id IS NOT NULL) THEN
        UPDATE public.shippers 
        SET delivery_status = 'available'
        WHERE user_id = NEW.shipper_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger chạy sau khi update cột status trên bảng orders
DROP TRIGGER IF EXISTS tr_release_shipper_on_order_end ON public.orders;
CREATE TRIGGER tr_release_shipper_on_order_end
    AFTER UPDATE OF status ON public.orders
    FOR EACH ROW
    WHEN (NEW.status IN ('delivered', 'cancelled'))
    EXECUTE FUNCTION public.release_shipper_on_order_end();

-- Cập nhật cho cả trường hợp gán đơn nhưng delivery_status chưa khớp (nếu cần)
COMMENT ON FUNCTION public.release_shipper_on_order_end() IS 'Giải phóng shipper khi đơn hàng kết thúc (delivered/cancelled)';
