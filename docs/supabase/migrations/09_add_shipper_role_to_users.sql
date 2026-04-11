-- Migration 011: Add shipper role to users table
-- Purpose: Allow creating users with role='shipper' for delivery staff

ALTER TABLE public.users 
    DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE public.users 
    ADD CONSTRAINT users_role_check 
    CHECK (role IN ('customer', 'staff', 'admin', 'shipper'));