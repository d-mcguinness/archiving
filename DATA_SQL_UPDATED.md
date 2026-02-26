# Data.sql Updated for TenantId and OwnerId ✅

## Summary
Updated `data.sql` to include the new `tenant_id` column in all archive INSERT statements, maintaining the existing `owner_id` column for clear separation between organization and user ownership.

---

## Changes Made

### Archive INSERT Statements Updated

All 12 archive records now include both `tenant_id` and `owner_id`:

```sql
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, ...)
```

### Data Mapping

#### Tenant 1 (Acme Corp)
- **Archives 1, 2, 10**: tenant_id=1, owner_id=1 (John Doe)

#### Tenant 2 (Tech Innovations)
- **Archives 3, 4, 11**: tenant_id=2, owner_id=2 (Jane Smith)

#### Tenant 3 (Global Services)
- **Archives 5, 6, 12**: tenant_id=3, owner_id=3 (Bob Johnson)

#### Tenant 4 (Startup Labs)
- **Archives 7, 8**: tenant_id=4, owner_id=4 (Alice Williams)
- **Archive 9**: tenant_id=4, owner_id=5 (Charlie Brown)

---

## Archive Distribution by Tenant

```
Tenant 1 (Acme Corp):          3 archives
Tenant 2 (Tech Innovations):   3 archives
Tenant 3 (Global Services):    3 archives
Tenant 4 (Startup Labs):       3 archives
----------------------------------------
Total:                        12 archives
```

---

## Archive Standards Coverage

Each tenant has archives with different standards:

**Tenant 1 (Acme Corp)**:
- NOARK5: Financial Reports (Published)
- OAIS: Budget (Published)
- NOARK5: Project Template (Draft)

**Tenant 2 (Tech Innovations)**:
- PREMIS: Digital Assets (Published)
- DUBLIN_CORE: Research Publications (Published)
- OAIS: Marketing Materials (Archived)

**Tenant 3 (Global Services)**:
- METS: Historical Documents (Published)
- EAD: Archives Finding Aid (Published)
- PREMIS: Training Videos (Published)

**Tenant 4 (Startup Labs)**:
- BAGIT: Product Development (Published)
- ISADG: Organizational Records (Published)
- MODS: Library Catalog (Published)

---

## Field Clarification

### In INSERT Statements

```sql
tenant_id = Organization/Tenant that owns the archive
owner_id  = User who owns/created the archive
```

### Example

```sql
INSERT INTO archives (id, tenant_id, owner_id, ...)
SELECT 1, 1, 1, ...  -- Archive 1: Tenant 1, Owner User 1
```

This means:
- Archive belongs to **Tenant 1** (Acme Corp)
- Archive is owned by **User 1** (John Doe)

---

## Data Consistency

All archives maintain:
- ✅ Valid tenant_id (1-4, matching existing tenants)
- ✅ Valid owner_id (1-5, matching existing users)
- ✅ User-tenant relationships preserved
- ✅ User assignments still reference the same users

---

## User Assignments

User assignments remain unchanged - they reference the archive_id and user_id as before. The user assignments create the detailed access control:

- **OWNER role**: Primary owner (matches owner_id)
- **EDITOR role**: Can edit the archive
- **VIEWER role**: Can view the archive

Example:
```sql
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 1, 1, 'OWNER', CURRENT_TIMESTAMP ...
```

---

## Testing Queries

### Get Archives by Tenant
```sql
SELECT id, tenant_id, owner_id, title, status 
FROM archives 
WHERE tenant_id = 1;

-- Expected: 3 archives (IDs 1, 2, 10)
```

### Get Archives by Owner
```sql
SELECT id, tenant_id, owner_id, title, status 
FROM archives 
WHERE owner_id = 2;

-- Expected: 3 archives (IDs 3, 4, 11)
```

### Get Archives by Tenant and Status
```sql
SELECT id, tenant_id, owner_id, title 
FROM archives 
WHERE tenant_id = 2 AND status = 'PUBLISHED';

-- Expected: 2 archives (IDs 3, 4)
```

### Verify Tenant-Archive Distribution
```sql
SELECT tenant_id, COUNT(*) as archive_count
FROM archives
GROUP BY tenant_id
ORDER BY tenant_id;

-- Expected:
-- 1 | 3
-- 2 | 3
-- 3 | 3
-- 4 | 3
```

---

## Migration Path

If you're loading this data fresh:

1. **Drop existing data** (if needed):
   ```sql
   TRUNCATE TABLE user_assignments CASCADE;
   TRUNCATE TABLE elements CASCADE;
   TRUNCATE TABLE archives CASCADE;
   ```

2. **Run updated data.sql**:
   ```bash
   psql -U username -d archiving < src/main/resources/data.sql
   ```

3. **Verify data**:
   ```sql
   SELECT COUNT(*) FROM archives;
   -- Expected: 12
   
   SELECT tenant_id, owner_id, title FROM archives ORDER BY id;
   -- Should show all tenant_id and owner_id populated
   ```

---

## Files Modified

1. ✅ `/src/main/resources/data.sql`
   - Updated all 12 archive INSERT statements
   - Added tenant_id column to each INSERT
   - Added comments clarifying tenant ownership
   - Maintained all other data unchanged

---

## Status: ✅ COMPLETE

The `data.sql` file has been successfully updated to reflect the new archive schema with explicit `tenant_id` and `owner_id` fields. All 12 archives now have clear tenant and user ownership assignments.

**Ready to load into database!** 🚀

