-- =====================================================
-- Add coordinates to restaurants table
-- Date: 2026-04-14
-- =====================================================

ALTER TABLE public.restaurants 
ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

CREATE INDEX idx_restaurants_coordinates ON public.restaurants(latitude, longitude);

-- =====================================================
-- Update existing restaurant coordinates (example data - adjust as needed)
-- =====================================================

-- Example: Update coordinates for existing restaurants (replace with actual coordinates)
-- UPDATE restaurants SET latitude = 10.7769, longitude = 106.7000 WHERE name = 'Restaurant Name';