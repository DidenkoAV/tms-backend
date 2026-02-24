-- Add role column to users table for simplified queries
-- This is a denormalized field that duplicates the primary role from user_roles table
-- The many-to-many relationship in user_roles remains the source of truth

-- Add role column (nullable initially)
ALTER TABLE users ADD COLUMN role VARCHAR(32);

-- Populate role column with primary role from user_roles
-- Priority: ROLE_ADMIN > ROLE_USER
UPDATE users u
SET role = (
    SELECT r.name
    FROM user_roles ur
    JOIN roles r ON r.id = ur.role_id
    WHERE ur.user_id = u.id
    ORDER BY 
        CASE r.name
            WHEN 'ROLE_ADMIN' THEN 1
            WHEN 'ROLE_USER' THEN 2
            ELSE 3
        END
    LIMIT 1
);

-- Set default role for users without any roles (should not happen in production)
UPDATE users SET role = 'ROLE_USER' WHERE role IS NULL;

-- Make column NOT NULL after populating
ALTER TABLE users ALTER COLUMN role SET NOT NULL;

-- Add index for faster role-based queries
CREATE INDEX idx_users_role ON users(role);

-- Add comment
COMMENT ON COLUMN users.role IS 'Primary user role (denormalized from user_roles). ROLE_ADMIN takes precedence over ROLE_USER.';

