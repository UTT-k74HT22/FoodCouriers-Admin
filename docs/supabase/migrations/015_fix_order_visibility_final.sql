-- Migration 015: Fix Order Items and Logs Visibility for Admin and Shipper
-- Purpose: Ensure Admin and Shipper can see order details without recursion errors

-- 1. Ensure helper functions exist (Redefining safely)
CREATE SCHEMA IF NOT EXISTS private;

CREATE OR REPLACE FUNCTION private.get_current_user_id()
RETURNS UUID LANGUAGE sql SECURITY DEFINER SET search_path = '' AS $$
    SELECT id FROM public.users WHERE auth_id = auth.uid();
$$;

CREATE OR REPLACE FUNCTION private.get_current_user_role()
RETURNS TEXT LANGUAGE sql SECURITY DEFINER SET search_path = '' AS $$
    SELECT role FROM public.users WHERE auth_id = auth.uid();
$$;

GRANT EXECUTE ON FUNCTION private.get_current_user_id() TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION private.get_current_user_role() TO authenticated, service_role;

-- 2. Reset policies for order_items and order_status_logs
DROP POLICY IF EXISTS "order_items_select_own" ON public.order_items;
DROP POLICY IF EXISTS "order_items_select_admin" ON public.order_items;
DROP POLICY IF EXISTS "order_items_select_shipper" ON public.order_items;
DROP POLICY IF EXISTS "order_items_select_v2" ON public.order_items;

DROP POLICY IF EXISTS "order_status_logs_select" ON public.order_status_logs;
DROP POLICY IF EXISTS "order_status_logs_select_v2" ON public.order_status_logs;

-- 3. Define NEW policies for ORDER_ITEMS
-- Strategy: If you can see the Order, you can see its Items.
-- This uses the existing RLS on the 'orders' table.
CREATE POLICY "order_items_select_v3" ON public.order_items
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.orders o 
            WHERE o.id = order_items.order_id
        )
    );

-- 4. Define NEW policies for ORDER_STATUS_LOGS
CREATE POLICY "order_status_logs_select_v3" ON public.order_status_logs
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.orders o 
            WHERE o.id = order_status_logs.order_id
        )
    );

-- 5. Ensure Admin and Shipper have full SELECT access to Orders if not already set correctly
-- (Re-applying the core order visibility to be safe)
DROP POLICY IF EXISTS "orders_shipper_select" ON public.orders;
DROP POLICY IF EXISTS "orders_admin_select" ON public.orders;
DROP POLICY IF EXISTS "orders_customer_select" ON public.orders;

CREATE POLICY "orders_select_v3" ON public.orders
    FOR SELECT TO authenticated
    USING (
        private.get_current_user_role() = 'admin' -- Admin see all
        OR user_id = private.get_current_user_id() -- Customer see own
        OR (
            private.get_current_user_role() = 'shipper' -- Shipper see assigned or available
            AND (
                shipper_id = private.get_current_user_id() 
                OR (shipper_id IS NULL AND status IN ('confirmed', 'preparing', 'ready_for_pickup'))
            )
        )
    );

-- 6. Final Grant
GRANT SELECT ON public.orders TO authenticated;
GRANT SELECT ON public.order_items TO authenticated;
GRANT SELECT ON public.order_status_logs TO authenticated;
