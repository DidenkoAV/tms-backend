-- Add assigned_to column to cases table
-- This field stores the user ID of the person responsible for this test case

ALTER TABLE cases
ADD COLUMN assigned_to BIGINT;

-- Add foreign key constraint to users table
ALTER TABLE cases
ADD CONSTRAINT fk_cases_assigned_to
FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL;

-- Add index for better query performance when filtering by assignee
CREATE INDEX idx_cases_assigned_to ON cases(assigned_to);

-- Add comment for documentation
COMMENT ON COLUMN cases.assigned_to IS 'User ID of the person assigned to this test case';

