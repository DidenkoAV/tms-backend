-- Add group_type column to replace boolean 'personal' field
-- PERSONAL - automatically created for each user, cannot be deleted
-- SHARED - created by users for collaboration, can be deleted by owner

-- Step 1: Add new column with default value
ALTER TABLE groups ADD COLUMN group_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL';

-- Step 2: Migrate existing data
-- If personal = true -> PERSONAL
-- If personal = false -> SHARED
UPDATE groups SET group_type = 'PERSONAL' WHERE personal = true;
UPDATE groups SET group_type = 'SHARED' WHERE personal = false;

-- Step 3: Drop old boolean column
ALTER TABLE groups DROP COLUMN personal;

-- Step 4: Add index for faster queries on group type
CREATE INDEX idx_groups_type ON groups(group_type);

-- Step 5: Update unique constraint to use new column
-- Drop old constraint
DROP INDEX IF EXISTS uk_groups_owner_personal;

-- Create new unique partial index (only for PERSONAL groups)
CREATE UNIQUE INDEX uk_groups_owner_personal 
ON groups(owner_id) 
WHERE group_type = 'PERSONAL';

