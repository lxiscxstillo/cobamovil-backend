-- Extend pets table with richer health fields
ALTER TABLE pets ADD COLUMN IF NOT EXISTS vaccinations TEXT;
ALTER TABLE pets ADD COLUMN IF NOT EXISTS deworming TEXT;
ALTER TABLE pets ADD COLUMN IF NOT EXISTS medical_conditions TEXT;
ALTER TABLE pets ADD COLUMN IF NOT EXISTS last_groom_date DATE;

