-- Add updated_at column to groups table
ALTER TABLE groups ADD COLUMN updated_at TIMESTAMPTZ;

-- Create index for updated_at (useful for sorting/filtering)
CREATE INDEX idx_groups_updated_at ON groups(updated_at);

