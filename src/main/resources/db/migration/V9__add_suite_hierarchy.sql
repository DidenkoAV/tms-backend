-- Add hierarchical support to suites table
-- Allows suites to have parent suites (max 5 levels deep)

-- Add parent_id column
ALTER TABLE suites
ADD COLUMN parent_id BIGINT REFERENCES suites(id) ON DELETE CASCADE;

-- Add depth column to track nesting level (0 = root, max 4 = 5th level)
ALTER TABLE suites
ADD COLUMN depth INTEGER NOT NULL DEFAULT 0;

-- Add constraint to prevent more than 5 levels of nesting
ALTER TABLE suites
ADD CONSTRAINT chk_suite_max_depth CHECK (depth <= 4);

-- Create index for parent_id lookups
CREATE INDEX idx_suites_parent ON suites(parent_id);

-- Create index for depth queries
CREATE INDEX idx_suites_depth ON suites(depth);

-- Add unique constraint: suite name must be unique within same parent and project
CREATE UNIQUE INDEX uq_suites_name_parent_project 
ON suites(project_id, COALESCE(parent_id, 0), name) 
WHERE is_archived = FALSE;

COMMENT ON COLUMN suites.parent_id IS 'Parent suite ID for hierarchical structure';
COMMENT ON COLUMN suites.depth IS 'Nesting level: 0=root, 1=1st level child, max 4=5th level';

