-- Add tenant_id column to archives table
-- This creates a clear separation between:
--   - tenantId: The organization/tenant that owns the archive
--   - ownerId: The user who created/owns the archive

-- Add tenant_id column (nullable initially for migration)
ALTER TABLE archives ADD COLUMN tenant_id BIGINT;

-- Add index for tenant_id
CREATE INDEX idx_archive_tenant_id ON archives(tenant_id);

-- Add composite index for tenant_id and status
CREATE INDEX idx_archive_tenant_status ON archives(tenant_id, status);

-- Optional: Set tenant_id to match owner_id for existing records (if needed)
-- UPDATE archives SET tenant_id = owner_id WHERE tenant_id IS NULL;

-- After data migration, make tenant_id NOT NULL
-- ALTER TABLE archives ALTER COLUMN tenant_id SET NOT NULL;

-- Optional: Add foreign key constraint to tenants table
-- ALTER TABLE archives ADD CONSTRAINT fk_archive_tenant
--   FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

COMMENT ON COLUMN archives.tenant_id IS 'Tenant (organization) that owns this archive';
COMMENT ON COLUMN archives.owner_id IS 'User who created/owns this archive';

