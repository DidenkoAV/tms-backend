-- Add admin user with verified email
-- Password: Password123!

-- Create admin user (password: admin123)
-- BCrypt hash for "admin123" - generated with strength 10
INSERT INTO users (email, password, full_name, enabled, created_at)
VALUES (
    'admin@testcase.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'System Administrator',
    true,
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Assign ROLE_ADMIN and ROLE_USER to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.email = 'admin@testcase.com'
  AND r.name IN ('ROLE_ADMIN', 'ROLE_USER')
ON CONFLICT DO NOTHING;

-- Create personal group for admin user
INSERT INTO groups (name, owner_id, personal, created_at)
SELECT 
    'Personal group for admin@testcase.com',
    u.id,
    true,
    NOW()
FROM users u
WHERE u.email = 'admin@testcase.com'
  AND NOT EXISTS (
    SELECT 1 FROM groups g WHERE g.owner_id = u.id AND g.personal = true
  );

-- Create group membership for admin user
INSERT INTO group_memberships (group_id, user_id, role, status, created_at)
SELECT 
    g.id,
    u.id,
    'OWNER',
    'ACTIVE',
    NOW()
FROM users u
JOIN groups g ON g.owner_id = u.id AND g.personal = true
WHERE u.email = 'admin@testcase.com'
  AND NOT EXISTS (
    SELECT 1 FROM group_memberships gm 
    WHERE gm.group_id = g.id AND gm.user_id = u.id
  );

