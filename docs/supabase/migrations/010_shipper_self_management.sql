-- Migration 010: Shipper Self Management RLS
-- Purpose: Allow shippers to view and update their own profile and availability

-- Drop existing policies if they exist (to ensure a clean state)
DROP POLICY IF EXISTS "shippers_select" ON public.shippers;
DROP POLICY IF EXISTS "shippers_insert" ON public.shippers;
DROP POLICY IF EXISTS "shippers_update" ON public.shippers;
DROP POLICY IF EXISTS "shippers_delete" ON public.shippers;

-- 1. SELECT: Admin sees everyone, Shipper sees their own profile, Staff sees their restaurant's shippers
CREATE POLICY "shippers_select" ON public.shippers
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM users u
            WHERE u.auth_id = auth.uid() 
            AND (
                u.role = 'admin' 
                OR (u.role = 'shipper' AND u.id = shippers.user_id)
                OR (
                    u.role = 'staff' AND EXISTS (
                        SELECT 1 FROM restaurant_staff rs 
                        WHERE rs.user_id = u.id AND rs.restaurant_id = shippers.restaurant_id
                    )
                )
            )
        )
    );

-- 2. INSERT: Only Admin can create shipper profiles
CREATE POLICY "shippers_insert" ON public.shippers
    FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );

-- 3. UPDATE: Admin can update anything, Shipper can update their own availability and vehicle info
CREATE POLICY "shippers_update" ON public.shippers
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM users u
            WHERE u.auth_id = auth.uid() 
            AND (
                u.role = 'admin' 
                OR (u.role = 'shipper' AND u.id = shippers.user_id)
            )
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM users u
            WHERE u.auth_id = auth.uid() 
            AND (
                u.role = 'admin' 
                OR (u.role = 'shipper' AND u.id = shippers.user_id)
            )
        )
    );

-- 4. DELETE: Only Admin
CREATE POLICY "shippers_delete" ON public.shippers
    FOR DELETE USING (
        EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
    );
