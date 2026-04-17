-- Migration 018: Allow users to delete their own notifications
-- Purpose: The admin app notification screen supports "delete all" for the
-- current account. Existing policies allow select/update only.

DROP POLICY IF EXISTS "notifications_delete_own" ON public.notifications;

CREATE POLICY "notifications_delete_own" ON public.notifications
    FOR DELETE TO authenticated
    USING (
        user_id = private.get_current_user_id()
        OR user_id IN (SELECT id FROM public.users WHERE auth_id = auth.uid())
    );

GRANT DELETE ON public.notifications TO authenticated;
