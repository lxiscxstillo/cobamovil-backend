-- Agregar campos de seguridad a la tabla users
ALTER TABLE users 
ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER',
ADD COLUMN account_non_expired BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN account_non_locked BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT true;

-- Actualizar el usuario admin existente para que tenga rol ADMIN
UPDATE users 
SET role = 'ADMIN' 
WHERE username = 'admin';

-- Crear índice en el campo role para mejor performance
CREATE INDEX idx_users_role ON users(role);