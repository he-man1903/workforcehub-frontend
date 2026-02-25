-- ============================================================
-- WorkforceHub Platform — Multi-Tenant PostgreSQL Schema
-- Java entity canonical names are authoritative.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- upload_jobs
-- ============================================================
CREATE TABLE IF NOT EXISTS upload_jobs (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         VARCHAR(128) NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    file_type         VARCHAR(20)  NOT NULL CHECK (file_type IN ('CSV', 'EXCEL')),
    status            VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'PARTIAL')),
    total_rows        INT,
    processed_rows    INT          DEFAULT 0,
    failed_rows       INT          DEFAULT 0,
    error_message     TEXT,
    created_by        VARCHAR(255),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_upload_jobs_tenant_id     ON upload_jobs(tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_upload_jobs_tenant_status ON upload_jobs(tenant_id, status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_upload_jobs_created       ON upload_jobs(created_at DESC);

-- ============================================================
-- employees
-- ============================================================
CREATE TABLE IF NOT EXISTS employees (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     VARCHAR(128) NOT NULL,
    upload_job_id UUID         NOT NULL REFERENCES upload_jobs(id),
    first_name    VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(320) NOT NULL,
    department    VARCHAR(255),
    job_title     VARCHAR(255),
    hire_date     DATE,
    status        VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (tenant_id, email)
);

CREATE INDEX IF NOT EXISTS idx_employees_tenant_id     ON employees(tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_employees_tenant_status ON employees(tenant_id, status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_employees_tenant_dept   ON employees(tenant_id, department) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_employees_upload_job_id ON employees(upload_job_id);

-- ============================================================
-- upload_job_rows
-- ============================================================
CREATE TABLE IF NOT EXISTS upload_job_rows (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     VARCHAR(128) NOT NULL,
    upload_job_id UUID         NOT NULL REFERENCES upload_jobs(id),
    row_number    INT          NOT NULL,
    row_status    VARCHAR(20)  NOT NULL CHECK (row_status IN ('PENDING', 'PROCESSED', 'FAILED', 'SKIPPED')),
    raw_data      JSONB,
    error_message TEXT,
    employee_id   UUID         REFERENCES employees(id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_upload_job_rows_tenant_id ON upload_job_rows(tenant_id);
CREATE INDEX IF NOT EXISTS idx_upload_job_rows_job_id    ON upload_job_rows(upload_job_id);
CREATE INDEX IF NOT EXISTS idx_upload_job_rows_status    ON upload_job_rows(row_status);

-- ============================================================
-- auth_users — OAuth identity store
-- Managed by: workforce-auth-service
-- Entity:     com.workforce.auth.domain.AuthUser
--
-- Column mapping:
--   google_subject → AuthUser.googleSubject  (stable Google sub claim)
--   email          → AuthUser.email
--   name           → AuthUser.name
--   picture_url    → AuthUser.pictureUrl
--   tenant_id      → AuthUser.tenantId       (derived from hd claim or google sub)
--   role           → AuthUser.role           (USER | ADMIN | SUPER_ADMIN)
--   active         → AuthUser.active         (soft-disable without deleting)
--   last_login_at  → AuthUser.lastLoginAt
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_users (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    google_subject VARCHAR(255) NOT NULL UNIQUE,
    email          VARCHAR(320) NOT NULL UNIQUE,
    name           VARCHAR(500),
    picture_url    VARCHAR(2048),
    tenant_id      VARCHAR(128) NOT NULL,
    role           VARCHAR(50)  NOT NULL DEFAULT 'USER'
                   CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN')),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_auth_users_google_subject ON auth_users(google_subject);
CREATE INDEX IF NOT EXISTS idx_auth_users_email          ON auth_users(email);
CREATE INDEX IF NOT EXISTS idx_auth_users_tenant_id      ON auth_users(tenant_id);

-- ============================================================
-- Row-level security (belt-and-suspenders tenant isolation)
-- Application must SET app.current_tenant = 'tenant-id' before queries
-- ============================================================
ALTER TABLE upload_jobs     ENABLE ROW LEVEL SECURITY;
ALTER TABLE employees       ENABLE ROW LEVEL SECURITY;
ALTER TABLE upload_job_rows ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_upload_jobs ON upload_jobs
    USING (tenant_id = current_setting('app.current_tenant', true));

CREATE POLICY tenant_isolation_employees ON employees
    USING (tenant_id = current_setting('app.current_tenant', true));

CREATE POLICY tenant_isolation_upload_job_rows ON upload_job_rows
    USING (tenant_id = current_setting('app.current_tenant', true));

-- ============================================================
-- Auto-update updated_at trigger
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_upload_jobs_updated_at
    BEFORE UPDATE ON upload_jobs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE TRIGGER trg_employees_updated_at
    BEFORE UPDATE ON employees
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE TRIGGER trg_upload_job_rows_updated_at
    BEFORE UPDATE ON upload_job_rows
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE TRIGGER trg_auth_users_updated_at
    BEFORE UPDATE ON auth_users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
