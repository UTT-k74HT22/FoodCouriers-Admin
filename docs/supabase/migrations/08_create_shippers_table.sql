-- Migration 08: Create shippers table
-- Purpose: Store shipper information with restaurant association

CREATE TABLE IF NOT EXISTS public.shippers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    restaurant_id UUID REFERENCES public.restaurants(id) ON DELETE SET NULL,
    license_plate TEXT,
    vehicle_type TEXT,
    is_available BOOLEAN NOT NULL DEFAULT true,
    is_active BOOLEAN NOT NULL DEFAULT true,
    total_delivered INTEGER DEFAULT 0,
    total_revenue BIGINT DEFAULT 0,
    rating DECIMAL(3,2),
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

CREATE INDEX IF NOT EXISTS idx_shippers_user_id ON public.shippers(user_id);
CREATE INDEX IF NOT EXISTS idx_shippers_restaurant_id ON public.shippers(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_shippers_is_available ON public.shippers(is_available);
CREATE INDEX IF NOT EXISTS idx_shippers_is_active ON public.shippers(is_active);

-- Enable RLS
ALTER TABLE public.shippers ENABLE ROW LEVEL SECURITY;

-- Add trigger for updated_at
DROP TRIGGER IF EXISTS update_shippers_updated_at ON shippers;
CREATE TRIGGER update_shippers_updated_at
    BEFORE UPDATE ON shippers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
