-- Drop unused group_invitations table
-- This table was created but never used in the application
-- Invitations are handled through group_memberships with PENDING status instead

DROP TABLE IF EXISTS group_invitations CASCADE;

