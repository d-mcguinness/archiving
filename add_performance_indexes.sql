-- Performance Optimization: Database Indexes
-- This migration adds strategic indexes to improve query performance
-- Run this script after the application has created the initial schema

-- Archive indexes (most important for performance)
CREATE INDEX IF NOT EXISTS idx_archive_tenant_id ON archives(tenant_id);
CREATE INDEX IF NOT EXISTS idx_archive_owner_id ON archives(owner_id);
CREATE INDEX IF NOT EXISTS idx_archive_status ON archives(status);
CREATE INDEX IF NOT EXISTS idx_archive_standard ON archives(standard);
CREATE INDEX IF NOT EXISTS idx_archive_created_at ON archives(created_at);
CREATE INDEX IF NOT EXISTS idx_archive_updated_at ON archives(updated_at);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_archive_tenant_status ON archives(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_archive_owner_status ON archives(owner_id, status);
CREATE INDEX IF NOT EXISTS idx_archive_tenant_created ON archives(tenant_id, created_at DESC);

-- Text search optimization
CREATE INDEX IF NOT EXISTS idx_archive_title_trgm ON archives USING gin (title gin_trgm_ops);

-- User indexes
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_name ON users(name);

-- Tenant indexes
CREATE INDEX IF NOT EXISTS idx_tenant_domain ON tenants(domain);
CREATE INDEX IF NOT EXISTS idx_tenant_subscription_tier ON tenants(subscription_tier);

-- User-Tenant relationship indexes
CREATE INDEX IF NOT EXISTS idx_user_tenant_tenant_id ON user_tenant(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_tenant_user_id ON user_tenant(user_id);

-- Element indexes
CREATE INDEX IF NOT EXISTS idx_elements_archive_id ON elements(archive_id);
CREATE INDEX IF NOT EXISTS idx_elements_parent_id ON elements(parent_id);
CREATE INDEX IF NOT EXISTS idx_elements_status ON elements(status);
CREATE INDEX IF NOT EXISTS idx_elements_created_at ON elements(created_at);

-- Document indexes
CREATE INDEX IF NOT EXISTS idx_documents_user_id ON documents(user_id);
CREATE INDEX IF NOT EXISTS idx_documents_tenant_id ON documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at DESC);

-- SIP indexes
CREATE INDEX IF NOT EXISTS idx_sips_tenant_id ON sips(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sips_owner_id ON sips(owner_id);
CREATE INDEX IF NOT EXISTS idx_sips_status ON sips(status);
CREATE INDEX IF NOT EXISTS idx_sips_standard ON sips(standard);

-- AIP indexes
CREATE INDEX IF NOT EXISTS idx_aips_tenant_id ON aips(tenant_id);
CREATE INDEX IF NOT EXISTS idx_aips_owner_id ON aips(owner_id);
CREATE INDEX IF NOT EXISTS idx_aips_status ON aips(status);
CREATE INDEX IF NOT EXISTS idx_aips_standard ON aips(standard);

-- DIP indexes
CREATE INDEX IF NOT EXISTS idx_dips_tenant_id ON dips(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dips_owner_id ON dips(owner_id);
CREATE INDEX IF NOT EXISTS idx_dips_status ON dips(status);
CREATE INDEX IF NOT EXISTS idx_dips_standard ON dips(standard);

-- Field indexes
CREATE INDEX IF NOT EXISTS idx_fields_element_id ON fields(element_id);

-- Archive-User assignment indexes
CREATE INDEX IF NOT EXISTS idx_archive_users_archive_id ON archive_users(archive_id);
CREATE INDEX IF NOT EXISTS idx_archive_users_user_id ON archive_users(user_id);

-- Analyze tables to update statistics after creating indexes
ANALYZE archives;
ANALYZE users;
ANALYZE tenants;
ANALYZE user_tenant;
ANALYZE elements;
ANALYZE documents;
ANALYZE sips;
ANALYZE aips;
ANALYZE dips;
ANALYZE fields;
ANALYZE archive_users;

-- Note: For text search on archive titles, you may need to enable pg_trgm extension first:
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;

