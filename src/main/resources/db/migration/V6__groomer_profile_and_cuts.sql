-- Groomer profiles
CREATE TABLE IF NOT EXISTS groomer_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    avatar_url VARCHAR(255),
    bio VARCHAR(500),
    specialties VARCHAR(255)
);

-- Cut history records
CREATE TABLE IF NOT EXISTS cut_records (
    id BIGSERIAL PRIMARY KEY,
    groomer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    service_type VARCHAR(30) NOT NULL,
    pet_name VARCHAR(60),
    date DATE,
    time TIME,
    notes VARCHAR(500),
    photo_url VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_cuts_groomer_date ON cut_records(groomer_id, date);
