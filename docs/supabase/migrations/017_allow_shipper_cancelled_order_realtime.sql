-- Migration 017: Allow shippers to receive realtime cancellation updates
-- Purpose:
-- Supabase Realtime applies SELECT RLS to updated rows. If an unassigned order
-- changes from confirmed/preparing/ready_for_pickup to cancelled, the previous
-- shipper policy no longer allows shippers to see that row, so no UPDATE event
-- is delivered to shipper clients. The app still filters cancelled orders out
-- of shipper lists; this policy only keeps the row visible enough for realtime.

DROP POLICY IF EXISTS "orders_select_shipper" ON public.orders;
DROP POLICY IF EXISTS "orders_shipper_select" ON public.orders;
DROP POLICY IF EXISTS "orders_select_v3" ON public.orders;

CREATE POLICY "orders_select_v4" ON public.orders
    FOR SELECT TO authenticated
    USING (
        private.get_current_user_role() = 'admin'
        OR user_id = private.get_current_user_id()
        OR (
            private.get_current_user_role() = 'shipper'
            AND (
                shipper_id = private.get_current_user_id()
                OR (
                    shipper_id IS NULL
                    AND status IN ('confirmed', 'preparing', 'ready_for_pickup', 'cancelled')
                )
            )
        )
    );

GRANT SELECT ON public.orders TO authenticated;
