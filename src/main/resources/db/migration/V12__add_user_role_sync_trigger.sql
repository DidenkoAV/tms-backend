-- Add PostgreSQL trigger to automatically sync users.role column when user_roles table changes
-- This ensures the denormalized role column is always in sync with the source of truth

-- Function to sync user role when user_roles changes
CREATE OR REPLACE FUNCTION sync_user_role()
RETURNS TRIGGER AS $$
DECLARE
    target_user_id BIGINT;
    new_role VARCHAR(32);
BEGIN
    -- Determine which user_id to update
    IF TG_OP = 'DELETE' THEN
        target_user_id := OLD.user_id;
    ELSE
        target_user_id := NEW.user_id;
    END IF;

    -- Calculate the primary role for this user
    -- Priority: ROLE_ADMIN > ROLE_USER
    SELECT r.name INTO new_role
    FROM user_roles ur
    JOIN roles r ON r.id = ur.role_id
    WHERE ur.user_id = target_user_id
    ORDER BY 
        CASE r.name
            WHEN 'ROLE_ADMIN' THEN 1
            WHEN 'ROLE_USER' THEN 2
            ELSE 3
        END
    LIMIT 1;

    -- If user has no roles, set default to ROLE_USER
    IF new_role IS NULL THEN
        new_role := 'ROLE_USER';
    END IF;

    -- Update the denormalized role column
    UPDATE users
    SET role = new_role
    WHERE id = target_user_id;

    RETURN NULL; -- For AFTER trigger, return value is ignored
END;
$$ LANGUAGE plpgsql;

-- Create trigger on INSERT
CREATE TRIGGER trigger_sync_user_role_insert
AFTER INSERT ON user_roles
FOR EACH ROW
EXECUTE FUNCTION sync_user_role();

-- Create trigger on UPDATE
CREATE TRIGGER trigger_sync_user_role_update
AFTER UPDATE ON user_roles
FOR EACH ROW
EXECUTE FUNCTION sync_user_role();

-- Create trigger on DELETE
CREATE TRIGGER trigger_sync_user_role_delete
AFTER DELETE ON user_roles
FOR EACH ROW
EXECUTE FUNCTION sync_user_role();

-- Add comment
COMMENT ON FUNCTION sync_user_role() IS 'Automatically syncs users.role column when user_roles table is modified. Ensures denormalized role is always in sync.';

