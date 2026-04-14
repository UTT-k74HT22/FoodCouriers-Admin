-- Migration 013 (REFACTORED): Shipper Order and Customer Visibility Policies
-- Purpose: Allow shippers to see orders available for delivery, their assigned orders, and basic customer contact info
-- Fix: Prevent infinite recursion in users table policies

-- 1. Create helper functions in 'private' schema (SECURITY DEFINER to bypass RLS)
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

GRANT EXECUTE ON FUNCTION private.get_current_user_id() TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION private.is_current_user_shipper() TO authenticated, service_role;

-- 2. DROP existing policies (including the recursive one)
DROP POLICY IF EXISTS "orders_select_shipper" ON public.orders;
DROP POLICY IF EXISTS "order_items_select_shipper" ON public.order_items;
DROP POLICY IF EXISTS "order_status_logs_select_shipper" ON public.order_status_logs;
DROP POLICY IF EXISTS "users_select_shipper_view_customer" ON public.users;

-- 3. ORDERS: Shipper can see orders assigned to them OR available for pickup
CREATE POLICY "orders_select_shipper" ON public.orders
    FOR SELECT TO authenticated
    USING (
        (SELECT private.is_current_user_shipper())
        AND (
            orders.shipper_id = (SELECT private.get_current_user_id()) -- Assigned to them
            OR (
                orders.shipper_id IS NULL 
                AND orders.status IN ('confirmed', 'preparing', 'ready_for_pickup')
                AND orders.delivery_status IN ('unassigned', 'searching')
            )
        )
    );

-- 4. ORDER_ITEMS: Shipper can see items for orders they can see
-- This works because 'orders' already has RLS, but for clarity:
CREATE POLICY "order_items_select_shipper" ON public.order_items
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.id = order_items.order_id
        )
    );

-- 5. ORDER_STATUS_LOGS: Shipper can see logs for orders they can see
CREATE POLICY "order_status_logs_select_shipper" ON public.order_status_logs
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.id = order_status_logs.order_id
        )
    );

-- 6. USERS (Customer Info): Shipper needs to see customer name and phone
-- Fix: Use helper function to avoid direct recursive SELECT on 'users' table
CREATE POLICY "users_select_shipper_view_customer" ON public.users
    FOR SELECT TO authenticated
    USING (
        (SELECT private.is_current_user_shipper())
        AND EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.user_id = users.id 
            AND o.shipper_id = (SELECT private.get_current_user_id())
        )
    );

-- 7. Ensure necessary permissions
GRANT SELECT ON public.orders TO authenticated;
GRANT SELECT ON public.order_items TO authenticated;
GRANT SELECT ON public.order_status_logs TO authenticated;
GRANT SELECT ON public.users TO authenticated;
