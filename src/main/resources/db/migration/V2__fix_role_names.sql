-- Fix role names to include ROLE_ prefix
-- This migration updates existing roles and ensures correct naming

-- Delete old roles if they exist
DELETE FROM user_roles;
DELETE FROM roles WHERE name IN ('ADMIN', 'USER');

-- Insert roles with correct names
INSERT INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_USER')
ON CONFLICT (name) DO NOTHING;

