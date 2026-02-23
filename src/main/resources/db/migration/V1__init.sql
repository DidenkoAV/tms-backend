-- =====================================================================
-- Test Case Management System - Database Schema
-- PostgreSQL 14+
-- =====================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================================
-- USERS & AUTHENTICATION
-- =====================================================================

-- User roles (ADMIN, USER)
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE
);

-- Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX idx_users_email ON users(email);

-- User-Role mapping (many-to-many)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- Email verification and password reset tokens
CREATE TABLE verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(32) NOT NULL,  -- EMAIL_VERIFICATION, PASSWORD_RESET, GROUP_INVITE
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verification_tokens_hash_type ON verification_tokens(token_hash, type);
CREATE INDEX idx_verification_tokens_expires ON verification_tokens(expires_at);
CREATE INDEX idx_verification_tokens_user ON verification_tokens(user_id);
CREATE INDEX idx_verification_tokens_unused ON verification_tokens(used_at) WHERE used_at IS NULL;

-- Personal Access Tokens (API tokens)
CREATE TABLE api_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    token_prefix VARCHAR(32) NOT NULL UNIQUE,
    secret_hash VARCHAR(128) NOT NULL,
    scopes VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_api_tokens_prefix ON api_tokens(token_prefix);
CREATE INDEX idx_api_tokens_user ON api_tokens(user_id);

-- Password change audit log
CREATE TABLE password_change_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_change_log_user_time ON password_change_log(user_id, created_at);

-- =====================================================================
-- GROUPS & MEMBERSHIPS
-- =====================================================================

-- Groups (collaborative workspaces)
CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    personal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_groups_owner ON groups(owner_id);
CREATE INDEX idx_groups_personal ON groups(personal);

-- Group memberships
CREATE TABLE group_memberships (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,  -- OWNER, MEMBER
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, PENDING, REMOVED
    invited_by_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_group_memberships_group_user UNIQUE (group_id, user_id)
);

CREATE INDEX idx_group_memberships_group ON group_memberships(group_id);
CREATE INDEX idx_group_memberships_user ON group_memberships(user_id);
CREATE INDEX idx_group_memberships_status ON group_memberships(status);

-- Group invitations
CREATE TABLE group_invitations (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    inviter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invitee_email VARCHAR(320) NOT NULL,
    invitee_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    token_hash VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',  -- PENDING, ACCEPTED, CANCELLED, EXPIRED
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT uk_invites_group_email_pending UNIQUE (group_id, invitee_email, status)
);

CREATE INDEX ix_invites_group_id ON group_invitations(group_id);
CREATE INDEX ix_invites_invitee_email ON group_invitations(invitee_email);
CREATE INDEX ix_invites_status ON group_invitations(status);
CREATE INDEX ix_invites_token_hash ON group_invitations(token_hash);

-- User preferences (stores current group selection and other settings)
CREATE TABLE user_preferences (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    current_group_id BIGINT REFERENCES groups(id) ON DELETE SET NULL,
    theme VARCHAR(16) DEFAULT 'light',
    language VARCHAR(8) DEFAULT 'en',
    timezone VARCHAR(64) DEFAULT 'UTC',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_preferences_current_group ON user_preferences(current_group_id);

-- =====================================================================
-- PROJECTS & TEST ORGANIZATION
-- =====================================================================

-- Projects (belong to groups)
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(32) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    archived_at TIMESTAMPTZ,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_projects_group_code UNIQUE (group_id, code)
);

CREATE INDEX idx_projects_group ON projects(group_id);
CREATE INDEX idx_projects_archived ON projects(is_archived);

-- Test Suites (collections of test cases)
CREATE TABLE suites (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    archived_at TIMESTAMPTZ,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_suites_project ON suites(project_id);
CREATE INDEX idx_suites_archived ON suites(is_archived);

-- Sections (hierarchical organization within suites)
CREATE TABLE sections (
    id BIGSERIAL PRIMARY KEY,
    suite_id BIGINT NOT NULL REFERENCES suites(id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES sections(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sort_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sections_suite ON sections(suite_id);
CREATE INDEX idx_sections_parent ON sections(parent_id);

-- =====================================================================
-- TEST CASE DICTIONARIES
-- =====================================================================

-- Test case types (Functional, Regression, Smoke, etc.)
CREATE TABLE case_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Test case priorities (Low, Medium, High, Critical)
CREATE TABLE priorities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    weight INT NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_priorities_weight CHECK (weight >= 0)
);

-- Ensure only one default priority
CREATE UNIQUE INDEX uk_priorities_default ON priorities(is_default) WHERE is_default = TRUE;

-- Test execution statuses (Passed, Failed, Blocked, etc.)
CREATE TABLE statuses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    color VARCHAR(16),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Ensure only one default status
CREATE UNIQUE INDEX uk_statuses_default ON statuses(is_default) WHERE is_default = TRUE;

-- =====================================================================
-- TEST CASES
-- =====================================================================

-- Test Cases
CREATE TABLE cases (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    suite_id BIGINT REFERENCES suites(id) ON DELETE SET NULL,
    section_id BIGINT REFERENCES sections(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    preconditions TEXT,
    type_id BIGINT REFERENCES case_types(id) ON DELETE SET NULL,
    priority_id BIGINT REFERENCES priorities(id) ON DELETE SET NULL,
    estimate_seconds INT DEFAULT 0,
    sort_index INT NOT NULL DEFAULT 0,

    -- Extended fields
    steps JSONB NOT NULL DEFAULT '[]',
    expected_result TEXT,
    actual_result TEXT,
    attachments JSONB NOT NULL DEFAULT '[]',
    test_data TEXT,
    autotest_mapping JSONB NOT NULL DEFAULT '{}',

    -- Status and classification
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    severity VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    automation_status VARCHAR(32) NOT NULL DEFAULT 'NOT_AUTOMATED',
    tags TEXT[] NOT NULL DEFAULT '{}',

    -- Audit fields
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    archived_at TIMESTAMPTZ,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,

    -- Constraints
    CONSTRAINT chk_cases_estimate CHECK (estimate_seconds >= 0),
    CONSTRAINT chk_cases_status CHECK (status IN ('DRAFT', 'READY', 'IN_PROGRESS', 'BLOCKED', 'PASSED', 'FAILED', 'DEPRECATED')),
    CONSTRAINT chk_cases_severity CHECK (severity IN ('TRIVIAL', 'MINOR', 'NORMAL', 'MAJOR', 'CRITICAL', 'BLOCKER')),
    CONSTRAINT chk_cases_automation_status CHECK (automation_status IN ('NOT_AUTOMATED', 'IN_PROGRESS', 'AUTOMATED', 'OBSOLETE'))
);

CREATE INDEX idx_cases_project ON cases(project_id);
CREATE INDEX idx_cases_suite ON cases(suite_id);
CREATE INDEX idx_cases_section ON cases(section_id);
CREATE INDEX idx_cases_type ON cases(type_id);
CREATE INDEX idx_cases_priority ON cases(priority_id);
CREATE INDEX idx_cases_created_by ON cases(created_by);
CREATE INDEX idx_cases_archived ON cases(is_archived);
CREATE INDEX idx_cases_status ON cases(status);
CREATE INDEX idx_cases_automation_status ON cases(automation_status);

-- GIN indexes for JSONB and array fields
CREATE INDEX idx_cases_steps ON cases USING GIN(steps);
CREATE INDEX idx_cases_attachments ON cases USING GIN(attachments);
CREATE INDEX idx_cases_tags ON cases USING GIN(tags);

-- =====================================================================
-- MILESTONES
-- =====================================================================

-- Milestones (project phases/releases)
CREATE TABLE milestones (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_date TIMESTAMPTZ,
    due_date TIMESTAMPTZ,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    archived_at TIMESTAMPTZ,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_milestones_dates CHECK (due_date IS NULL OR start_date IS NULL OR due_date >= start_date)
);

CREATE INDEX idx_milestones_project ON milestones(project_id);
CREATE INDEX idx_milestones_created_by ON milestones(created_by);
CREATE INDEX idx_milestones_archived ON milestones(is_archived);
CREATE INDEX idx_milestones_dates ON milestones(start_date, due_date);

-- =====================================================================
-- TEST RUNS
-- =====================================================================

-- Test Runs (test execution sessions)
CREATE TABLE runs (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    archived_at TIMESTAMPTZ,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_runs_project ON runs(project_id);
CREATE INDEX idx_runs_created_by ON runs(created_by);
CREATE INDEX idx_runs_archived ON runs(is_archived);
CREATE INDEX idx_runs_closed ON runs(is_closed);

-- Milestone-Run association (many-to-many)
CREATE TABLE milestone_runs (
    milestone_id BIGINT NOT NULL REFERENCES milestones(id) ON DELETE CASCADE,
    run_id BIGINT NOT NULL REFERENCES runs(id) ON DELETE CASCADE,
    PRIMARY KEY (milestone_id, run_id)
);

CREATE INDEX idx_milestone_runs_milestone ON milestone_runs(milestone_id);
CREATE INDEX idx_milestone_runs_run ON milestone_runs(run_id);

-- Run Cases (test cases included in a run)
CREATE TABLE run_cases (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES runs(id) ON DELETE CASCADE,
    case_id BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    assignee_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    current_status_id BIGINT REFERENCES statuses(id) ON DELETE SET NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_run_cases_run_case UNIQUE (run_id, case_id)
);

CREATE INDEX idx_run_cases_run ON run_cases(run_id);
CREATE INDEX idx_run_cases_case ON run_cases(case_id);
CREATE INDEX idx_run_cases_assignee ON run_cases(assignee_id);
CREATE INDEX idx_run_cases_status ON run_cases(current_status_id);

-- Test Results (execution results for run cases)
CREATE TABLE results (
    id BIGSERIAL PRIMARY KEY,
    run_case_id BIGINT NOT NULL REFERENCES run_cases(id) ON DELETE CASCADE,
    status_id BIGINT NOT NULL REFERENCES statuses(id),
    comment TEXT,
    defects_json TEXT,
    elapsed_seconds INT,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_results_elapsed CHECK (elapsed_seconds IS NULL OR elapsed_seconds >= 0)
);

CREATE INDEX idx_results_run_case ON results(run_case_id);
CREATE INDEX idx_results_status ON results(status_id);
CREATE INDEX idx_results_created_by ON results(created_by);
CREATE INDEX idx_results_created_at ON results(created_at);

-- =====================================================================
-- JIRA INTEGRATION
-- =====================================================================

-- Jira connections (per group)
CREATE TABLE jira_connections (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    base_url TEXT NOT NULL,
    email TEXT NOT NULL,
    token_encrypted TEXT NOT NULL,
    default_project TEXT,
    default_issue_type TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jira_connections_group ON jira_connections(group_id);

-- Test Case - Jira Issue links
CREATE TABLE test_case_issues (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    issue_key TEXT NOT NULL,
    issue_url TEXT NOT NULL,
    status TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_test_case_issues_case ON test_case_issues(case_id);
CREATE INDEX idx_test_case_issues_key ON test_case_issues(issue_key);

-- =====================================================================
-- SEED DATA
-- =====================================================================

-- Insert default roles
INSERT INTO roles (name) VALUES ('ADMIN'), ('USER') ON CONFLICT DO NOTHING;

-- Insert default case types
INSERT INTO case_types (name, description) VALUES
    ('Functional', 'Functional testing'),
    ('Regression', 'Regression testing'),
    ('Smoke', 'Smoke sanity checks'),
    ('Integration', 'Integration flows'),
    ('API', 'API level tests'),
    ('UI', 'UI/UX level tests')
ON CONFLICT DO NOTHING;

-- Insert default priorities
INSERT INTO priorities (name, weight, is_default) VALUES
    ('Low', 1, FALSE),
    ('Medium', 2, TRUE),
    ('High', 3, FALSE),
    ('Critical', 4, FALSE)
ON CONFLICT DO NOTHING;

-- Insert default statuses
INSERT INTO statuses (name, color, is_default, is_final) VALUES
    ('Untested', '#6c757d', TRUE, FALSE),
    ('Passed', '#28a745', FALSE, TRUE),
    ('Failed', '#dc3545', FALSE, TRUE),
    ('Blocked', '#ffc107', FALSE, FALSE),
    ('Retest', '#17a2b8', FALSE, FALSE),
    ('Skipped', '#6c757d', FALSE, TRUE)
ON CONFLICT DO NOTHING;

