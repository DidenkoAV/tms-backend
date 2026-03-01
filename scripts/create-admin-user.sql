-- Usage:
-- psql "$DATABASE_URL" \
--   -v admin_email='admin@example.com' \
--   -v admin_password='StrongPass123!' \
--   -v admin_full_name='System Administrator' \
--   -f scripts/create-admin-user.sql
--
-- Notes:
-- - Script is idempotent: safe to run multiple times.
-- - Existing user with the same email is updated and elevated to ROLE_ADMIN.

\set ON_ERROR_STOP on

-- Ensure roles exist.
INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_USER') ON CONFLICT (name) DO NOTHING;

-- Create or update user and force admin role.
INSERT INTO users (email, password, full_name, enabled, role, created_at)
VALUES (
    lower(trim(:'admin_email')),
    crypt(:'admin_password', gen_salt('bf', 10)),
    COALESCE(NULLIF(:'admin_full_name', ''), 'System Administrator'),
    true,
    'ROLE_ADMIN',
    NOW()
)
ON CONFLICT (email) DO UPDATE
SET password = crypt(:'admin_password', gen_salt('bf', 10)),
    full_name = COALESCE(NULLIF(:'admin_full_name', ''), users.full_name),
    enabled = true,
    role = 'ROLE_ADMIN';

DROP TABLE IF EXISTS _admin_ctx;
CREATE TEMP TABLE _admin_ctx AS
SELECT id, email
FROM users
WHERE email = lower(trim(:'admin_email'));

-- Ensure role mappings.
INSERT INTO user_roles (user_id, role_id)
SELECT c.id, r.id
FROM _admin_ctx c
JOIN roles r ON r.name IN ('ROLE_ADMIN', 'ROLE_USER')
ON CONFLICT DO NOTHING;

-- Ensure personal group.
INSERT INTO groups (name, owner_id, group_type, created_at)
SELECT 'Personal group for ' || c.email, c.id, 'PERSONAL', NOW()
FROM _admin_ctx c
ON CONFLICT (owner_id) WHERE group_type = 'PERSONAL' DO NOTHING;

-- Ensure owner membership in personal group.
INSERT INTO group_memberships (group_id, user_id, role, status, created_at)
SELECT g.id, c.id, 'OWNER', 'ACTIVE', NOW()
FROM _admin_ctx c
JOIN groups g ON g.owner_id = c.id AND g.group_type = 'PERSONAL'
ON CONFLICT (group_id, user_id) DO UPDATE
SET role = 'OWNER',
    status = 'ACTIVE';

SELECT 'Admin ensured: email=' || c.email || ', user_id=' || c.id AS result
FROM _admin_ctx c;
