-- Remove default seeded admin user created by V3 migration.
-- Keep V3 unchanged for checksum stability; cleanup is done here.

-- Nullify inviter references first to avoid FK issues.
UPDATE group_memberships
SET invited_by_id = NULL
WHERE invited_by_id = (SELECT id FROM users WHERE email = 'admin@testcase.com');

-- Remove group memberships where seeded admin participates.
DELETE FROM group_memberships
WHERE user_id = (SELECT id FROM users WHERE email = 'admin@testcase.com');

-- Remove groups owned by seeded admin (cascades related memberships/invitations via FK definitions).
DELETE FROM groups
WHERE owner_id = (SELECT id FROM users WHERE email = 'admin@testcase.com');

-- Remove role links for seeded admin.
DELETE FROM user_roles
WHERE user_id = (SELECT id FROM users WHERE email = 'admin@testcase.com');

-- Finally remove seeded admin account.
DELETE FROM users
WHERE email = 'admin@testcase.com';
