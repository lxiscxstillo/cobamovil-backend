-- Add assigned groomer to bookings
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS assigned_groomer_id BIGINT;
ALTER TABLE bookings ADD CONSTRAINT fk_bookings_groomer FOREIGN KEY (assigned_groomer_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_bookings_groomer ON bookings(assigned_groomer_id);
