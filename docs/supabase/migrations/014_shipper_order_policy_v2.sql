-- Migration 014: Full RLS Reset and Fix for Users and Shippers
-- Purpose: Clear all existing policies on 'users' and 'shippers' to fix infinite recursion and restore correct access

-- 1. Redefine/Ensure helper functions exist in 'private' schema
CREATE SCHEMA IF NOT EXISTS private;

CREATE OR REPLACE FUNCTION private.get_current_user_id()
RETURNS UUID
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT id FROM public.users WHERE auth_id = auth.uid();
$$;

CREATE OR REPLACE FUNCTION private.get_current_user_role()
RETURNS TEXT
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT role FROM public.users WHERE auth_id = auth.uid();
$$;

GRANT EXECUTE ON FUNCTION private.get_current_user_id() TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION private.get_current_user_role() TO authenticated, service_role;

-- 2. Clean up ALL existing policies on users, shippers, and orders to start fresh
DO $$ 
DECLARE
    pol RECORD;
BEGIN
    FOR pol IN (SELECT policyname, tablename FROM pg_policies WHERE schemaname = 'public' AND tablename IN ('users', 'shippers', 'orders'))
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I', pol.policyname, pol.tablename);
    END LOOP;
END $$;

-- =====================================================
-- TABLE: users
-- =====================================================

-- Anyone can see their own profile
CREATE POLICY "users_self_select" ON public.users
    FOR SELECT TO authenticated
    USING (auth_id = auth.uid());

-- Admin can see everyone
CREATE POLICY "users_admin_select" ON public.users
    FOR SELECT TO authenticated
    USING (private.get_current_user_role() = 'admin');

-- Shippers can see customers of their assigned orders
CREATE POLICY "users_shipper_select_customers" ON public.users
    FOR SELECT TO authenticated
    USING (
        private.get_current_user_role() = 'shipper'
        AND EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.user_id = users.id 
            AND o.shipper_id = private.get_current_user_id()
        )
    );

-- Users can update their own profile
CREATE POLICY "users_self_update" ON public.users
    FOR UPDATE TO authenticated
    USING (auth_id = auth.uid())
    WITH CHECK (auth_id = auth.uid());

-- Admin can update anyone
CREATE POLICY "users_admin_update" ON public.users
    FOR UPDATE TO authenticated
    USING (private.get_current_user_role() = 'admin');

-- =====================================================
-- TABLE: shippers
-- =====================================================

-- Shippers can see their own profile
CREATE POLICY "shippers_self_select" ON public.shippers
    FOR SELECT TO authenticated
    USING (user_id = private.get_current_user_id());

-- Admin can see all shippers
CREATE POLICY "shippers_admin_select" ON public.shippers
    FOR SELECT TO authenticated
    USING (private.get_current_user_role() = 'admin');

-- Shippers can update their own status/vehicle info
CREATE POLICY "shippers_self_update" ON public.shippers
    FOR UPDATE TO authenticated
    USING (user_id = private.get_current_user_id());

-- Admin can manage all shippers
CREATE POLICY "shippers_admin_all" ON public.shippers
    FOR ALL TO authenticated
    USING (private.get_current_user_role() = 'admin');

-- =====================================================
-- TABLE: orders
-- =====================================================

-- Customers see their own orders
CREATE POLICY "orders_customer_select" ON public.orders
    FOR SELECT TO authenticated
    USING (user_id = private.get_current_user_id());

-- Admin see all orders
CREATE POLICY "orders_admin_select" ON public.orders
    FOR SELECT TO authenticated
    USING (private.get_current_user_role() = 'admin');

-- Shippers see assigned orders OR available orders
CREATE POLICY "orders_shipper_select" ON public.orders
    FOR SELECT TO authenticated
    USING (
        private.get_current_user_role() = 'shipper'
        AND (
            shipper_id = private.get_current_user_id()
            OR (
                shipper_id IS NULL 
                AND status IN ('confirmed', 'preparing', 'ready_for_pickup')
            )
        )
    );

-- Admin can update orders
CREATE POLICY "orders_admin_update" ON public.orders
    FOR UPDATE TO authenticated
    USING (private.get_current_user_role() = 'admin');

-- Customer can create orders
CREATE POLICY "orders_customer_insert" ON public.orders
    FOR INSERT TO authenticated
    WITH CHECK (user_id = private.get_current_user_id());

-- Ensure basic permissions are granted
GRANT SELECT, UPDATE ON public.users TO authenticated;
GRANT SELECT, UPDATE ON public.shippers TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.orders TO authenticated;
