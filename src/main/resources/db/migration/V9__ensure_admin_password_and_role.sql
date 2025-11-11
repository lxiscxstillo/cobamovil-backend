-- Ensure admin user has known password and ADMIN role (idempotent update)
UPDATE users
SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    role = 'ADMIN',
    enabled = true,
    account_non_locked = true,
    account_non_expired = true,
    credentials_non_expired = true,
    updated_at = NOW()
WHERE username = 'admin';

