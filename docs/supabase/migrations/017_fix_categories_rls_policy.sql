-- Migration: Update categories policy to use database role check
-- Date: 2026-04-15
-- Purpose: Fix RLS policy for categories using database role instead of JWT
create or replace function public.is_admin()
returns boolean
language sql
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.users
    where auth_id = auth.uid()
      and role = 'admin'
  );
$$;