-- Add unique constraint to prevent multiple personal groups per user
-- This prevents race conditions during user registration/OAuth login

-- First, clean up any duplicate personal groups (keep the oldest one)
DELETE FROM groups g1
WHERE g1.personal = true
  AND EXISTS (
    SELECT 1 FROM groups g2
    WHERE g2.owner_id = g1.owner_id
      AND g2.personal = true
      AND g2.id < g1.id
  );

-- Add unique partial index (only for personal groups)
CREATE UNIQUE INDEX uk_groups_owner_personal 
ON groups(owner_id) 
WHERE personal = true;

