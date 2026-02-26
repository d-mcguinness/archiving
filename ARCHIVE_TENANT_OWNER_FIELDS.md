# Archive TenantId and OwnerId Implementation ✅

## Summary
Added explicit `tenantId` field to Archive model while keeping `ownerId` field, creating a clear separation between:
- **tenantId**: The organization/tenant that owns the archive
- **ownerId**: The user who created/owns the archive

---

## Database Schema Changes

### Archive Table Structure

```sql
archives:
  - id (PK)
  - tenant_id (FK → tenants.id)   ← Organization owner
  - owner_id (FK → users.id)      ← User owner
  - title
  - description
  - content
  - created_at
  - updated_at
  - status
  - standard
```

### Relationships

```
Tenant (1) ←→ (N) Archives via tenant_id
  One tenant can have many archives
  Each archive belongs to one tenant

User (1) ←→ (N) Archives via owner_id
  One user can own many archives
  Each archive has one user owner
```

### Indexes Created

- `idx_archive_tenant_id` on `tenant_id`
- `idx_archive_owner_id` on `owner_id`
- `idx_archive_tenant_status` on `(tenant_id, status)` - Composite
- `idx_archive_owner_status` on `(owner_id, status)` - Composite

---

## Backend Changes

### 1. Archive Model (`Archive.java`)

**Added Field**:
```java
@Column(name = "tenant_id", nullable = false)
private Long tenantId;  // Tenant (organization) that owns this archive

@Column(name = "owner_id", nullable = false)
private Long ownerId;  // User who owns/created the archive
```

**Updated Constructor**:
```java
public Archive(Long id, Long tenantId, Long ownerId, String title, ...)
```

### 2. CreateArchiveInput (`CreateArchiveInput.java`)

**Fields**:
```java
@NotNull(message = "Tenant ID is required")
private Long tenantId;  // Tenant (organization) ID

@NotNull(message = "Owner ID (User ID) is required")
private Long ownerId;  // User ID - owner/creator of the archive

@NotNull(message = "User ID is required")
private Long userId;  // User ID for backward compatibility
```

### 3. ArchiveService (`ArchiveService.java`)

**Updated createArchive**:
```java
Archive archive = new Archive(
    null,
    input.getTenantId(),  // ← Tenant (organization)
    input.getOwnerId(),   // ← User owner
    input.getTitle(),
    // ... other fields
);

// Assign creator as OWNER role
archive.assignUser(input.getUserId(), UserRole.OWNER);
```

---

## Frontend Changes

### Create Archive Form (`/tenants/[id]/create/+page.svelte`)

**Form Data Structure**:
```typescript
let newArchive = {
  tenantId: data.tenantId,  // From route param
  ownerId: '',              // Selected user (owner)
  userId: '',               // Selected user (creator)
  title: '',
  description: '',
  content: '',
  standard: 'NOARK5'
};
```

**Form Fields**:
1. **Archive Owner** (ownerId) - User who owns the archive
2. **Creator / User** (userId) - User creating the archive
3. Title, Description, Content, Standard

**GraphQL Mutation**:
```graphql
mutation CreateArchive($input: CreateArchiveInput!) {
  createArchive(input: $input) {
    id
    tenantId    ← Organization
    ownerId     ← User owner
    title
    description
    content
    status
    standard
  }
}
```

**Variables**:
```json
{
  "input": {
    "tenantId": 1,      // Organization ID
    "ownerId": 5,       // User who owns it
    "userId": 5,        // User creating it
    "title": "Archive",
    "content": "...",
    "standard": "NOARK5"
  }
}
```

---

## Data Model Comparison

### Before
```
Archive:
  - ownerId (ambiguous - could be tenant or user)
```

### After
```
Archive:
  - tenantId (explicitly the organization/tenant)
  - ownerId (explicitly the user owner)
  - userId (creator, assigned as OWNER role)
```

---

## Use Cases

### Use Case 1: Tenant-Scoped Archive
```
Tenant: Acme Corporation (ID: 1)
Owner: John Doe (ID: 5)
Creator: John Doe (ID: 5)

Result:
  tenantId: 1  ← Organization
  ownerId: 5   ← User owner
  userId: 5    ← Creator
```

### Use Case 2: Different Owner and Creator
```
Tenant: Tech Inc (ID: 2)
Owner: Alice (ID: 10)
Creator: Bob (ID: 15)

Result:
  tenantId: 2   ← Organization
  ownerId: 10   ← Alice owns it
  userId: 15    ← Bob created it
```

---

## Query Examples

### Get Archives by Tenant
```sql
SELECT * FROM archives 
WHERE tenant_id = 1 
ORDER BY created_at DESC;
```

### Get Archives by Owner
```sql
SELECT * FROM archives 
WHERE owner_id = 5 
ORDER BY created_at DESC;
```

### Get Archives by Tenant and Owner
```sql
SELECT * FROM archives 
WHERE tenant_id = 1 
  AND owner_id = 5 
ORDER BY created_at DESC;
```

### Get Archives by Tenant and Status
```sql
SELECT * FROM archives 
WHERE tenant_id = 1 
  AND status = 'PUBLISHED'
ORDER BY created_at DESC;
```

---

## Migration Steps

### 1. Run SQL Migration
```bash
psql -U username -d archiving < add_tenant_id_to_archives.sql
```

### 2. Data Migration (if needed)
If you have existing archives and want to set tenantId based on ownerId:
```sql
-- Map owner_id to their tenant_id via user_tenant table
UPDATE archives a
SET tenant_id = (
  SELECT ut.tenant_id 
  FROM user_tenant ut 
  WHERE ut.user_id = a.owner_id 
  LIMIT 1
)
WHERE tenant_id IS NULL;
```

### 3. Make Column NOT NULL
After data migration:
```sql
ALTER TABLE archives 
ALTER COLUMN tenant_id SET NOT NULL;
```

### 4. Add Foreign Key (Optional)
```sql
ALTER TABLE archives 
ADD CONSTRAINT fk_archive_tenant 
FOREIGN KEY (tenant_id) 
REFERENCES tenants(id) 
ON DELETE CASCADE;
```

---

## Benefits

### Clear Separation of Concerns
✅ `tenantId` = Organization/Tenant ownership
✅ `ownerId` = User ownership
✅ No ambiguity about what each field represents

### Better Queries
✅ Easy to query archives by tenant
✅ Easy to query archives by user owner
✅ Composite indexes for fast tenant-scoped queries

### Flexible Ownership Model
✅ Archive owned by organization (tenant)
✅ Archive owned by user (owner)
✅ Different creator vs owner (userId vs ownerId)

### Multi-Tenancy Support
✅ Explicit tenant association
✅ Data isolation by tenant
✅ Scalable multi-tenant architecture

---

## Testing

### Test Archive Creation

1. **Login as TENANT**
   ```
   Username: tenant
   Password: tenant123
   ```

2. **Create Archive**
   - Navigate to: `/tenants/1/create`
   - Select Archive Owner: John Doe (ID: 5)
   - Select Creator: John Doe (ID: 5)
   - Fill in title and content
   - Click "Create Archive"

3. **Verify in Database**
   ```sql
   SELECT id, tenant_id, owner_id, title 
   FROM archives 
   ORDER BY id DESC 
   LIMIT 1;
   
   -- Expected:
   -- tenant_id: 1
   -- owner_id: 5
   ```

### Test Queries

1. **Get Tenant Archives**
   ```sql
   SELECT COUNT(*) FROM archives WHERE tenant_id = 1;
   ```

2. **Get User Owned Archives**
   ```sql
   SELECT COUNT(*) FROM archives WHERE owner_id = 5;
   ```

3. **Get Tenant Archives by Status**
   ```sql
   SELECT * FROM archives 
   WHERE tenant_id = 1 AND status = 'DRAFT';
   ```

---

## Files Modified

### Backend
1. ✅ `/src/main/java/.../archive/model/Archive.java`
   - Added `tenantId` field
   - Updated constructor
   - Added indexes

2. ✅ `/src/main/java/.../archive/input/CreateArchiveInput.java`
   - Added `tenantId` field
   - Kept `ownerId` and `userId` fields

3. ✅ `/src/main/java/.../archive/ArchiveService.java`
   - Updated `createArchive` to use both `tenantId` and `ownerId`

4. ✅ Created: `add_tenant_id_to_archives.sql`
   - Database migration script

### Frontend
5. ✅ `/frontend/src/routes/tenants/[id]/create/+page.svelte`
   - Added separate `ownerId` field
   - Updated mutation to send `tenantId`, `ownerId`, and `userId`
   - Updated form validation

---

## API Changes

### CreateArchive Mutation

**Before**:
```graphql
input CreateArchiveInput {
  ownerId: ID!   # Ambiguous - tenant or user?
  userId: ID!
  title: String!
  content: String
  standard: ArchiveStandard!
}
```

**After**:
```graphql
input CreateArchiveInput {
  tenantId: ID!  # Explicitly tenant/organization
  ownerId: ID!   # Explicitly user owner
  userId: ID!    # Creator user
  title: String!
  content: String
  standard: ArchiveStandard!
}
```

---

## Status: ✅ COMPLETE

Successfully added `tenantId` field to Archive model while keeping `ownerId` field:
- ✅ Database schema updated with new field and indexes
- ✅ Backend models updated (Archive, CreateArchiveInput, ArchiveService)
- ✅ Frontend updated with separate owner selection
- ✅ Clear separation: tenantId (org) vs ownerId (user)
- ✅ Migration script created
- ✅ Composite indexes for performance

**Archives now have explicit tenant and user ownership!** 🎉

**Ready for database migration and testing!** 🚀

