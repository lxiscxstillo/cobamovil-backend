-- Seed default admin user if missing (works even if baseline-on-migrate skipped V1 seed)
INSERT INTO users (username, email, password, role, account_non_expired, account_non_locked, credentials_non_expired, enabled, created_at, updated_at)
SELECT 'admin', 'admin@cobamovil.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'ADMIN', true, true, true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

