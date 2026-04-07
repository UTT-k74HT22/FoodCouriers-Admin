-- Migration 007: Add shipper role to restaurant_staff and shipper_id to orders
-- Purpose: Support restaurant-specific delivery staff (shipper = nhân viên giao hàng của nhà hàng)

-- 1. Add 'shipper' to restaurant_staff role_in_restaurant CHECK constraint
ALTER TABLE public.restaurant_staff
    DROP CONSTRAINT IF EXISTS restaurant_staff_role_in_restaurant_check;

ALTER TABLE public.restaurant_staff
    ADD CONSTRAINT restaurant_staff_role_in_restaurant_check
    CHECK (role_in_restaurant IN ('manager', 'operator', 'shipper'));

-- 2. Add shipper_id column to orders table
ALTER TABLE public.orders
    ADD COLUMN IF NOT EXISTS shipper_id UUID REFERENCES public.users(id) ON DELETE SET NULL;

-- 3. Add index for faster shipper order lookups
CREATE INDEX IF NOT EXISTS idx_orders_shipper_id ON public.orders(shipper_id);

-- 4. Add RLS policies for shippers
-- Shippers can only see orders assigned to them
CREATE POLICY "orders_select_shipper" ON public.orders
    FOR SELECT USING (
        shipper_id IN (
            SELECT user_id FROM public.restaurant_staff
            WHERE role_in_restaurant = 'shipper'
            AND user_id IN (SELECT id FROM public.users WHERE auth_id = auth.uid())
        )
    );

-- Shippers can update status of their assigned orders (delivering -> delivered only)
CREATE POLICY "orders_update_shipper" ON public.orders
    FOR UPDATE USING (
        shipper_id IN (
            SELECT user_id FROM public.restaurant_staff
            WHERE role_in_restaurant = 'shipper'
            AND user_id IN (SELECT id FROM public.users WHERE auth_id = auth.uid())
        )
    );

-- Shippers can see their own restaurant_staff record
CREATE POLICY "restaurant_staff_select_shipper" ON public.restaurant_staff
    FOR SELECT USING (
        user_id IN (SELECT id FROM public.users WHERE auth_id = auth.uid())
        OR role_in_restaurant = 'shipper'
    );
