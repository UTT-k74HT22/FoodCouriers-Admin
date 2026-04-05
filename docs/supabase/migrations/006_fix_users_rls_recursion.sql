-- =====================================================
-- Food Ordering App - Fix users RLS recursion
-- Version: 1.0
-- Date: 2026-04-04
-- Description: Replace self-referencing users policies with security definer helper.
-- =====================================================

CREATE SCHEMA IF NOT EXISTS private;

CREATE OR REPLACE FUNCTION private.is_current_user_admin()
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.users u
        WHERE u.auth_id = auth.uid()
          AND u.role = 'admin'
    );
$$;

REVOKE ALL ON FUNCTION private.is_current_user_admin() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION private.is_current_user_admin() TO authenticated, service_role;

DROP POLICY IF EXISTS "users_select_admin" ON public.users;
CREATE POLICY "users_select_admin" ON public.users
    FOR SELECT TO authenticated
    USING ((SELECT private.is_current_user_admin()));

DROP POLICY IF EXISTS "users_update_admin" ON public.users;
CREATE POLICY "users_update_admin" ON public.users
    FOR UPDATE TO authenticated
    USING ((SELECT private.is_current_user_admin()))
    WITH CHECK ((SELECT private.is_current_user_admin()));
