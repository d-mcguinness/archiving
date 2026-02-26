# Tenant Archive Creation at /tenants/[id]/create ✅

## Summary
Created a tenant-scoped archive creation page at `/tenants/[id]/create` that automatically associates archives with the tenant ID (as ownerId). ADMIN and TENANT users can create archives for tenants, with proper safeguards.

---

## Key Changes

### Frontend Changes

**Route Created**: `/tenants/[id]/create`
- New page for creating archives scoped to a specific tenant
- Archives are automatically associated with the tenant (ownerId = tenantId)
- 500+ lines with full functionality

**Updated**: `/tenants/[id]/archives/+page.svelte`
- Changed "+ Add Archive" button to point to `/tenants/{id}/create`
- Properly scoped to tenant context

### Backend Changes

**Updated**: `CreateArchiveInput.java`
- Added `ownerId` field (required)
- Archives now require both `ownerId` (tenant) and `userId` (creator)

**Updated**: `ArchiveService.java`
- Modified `createArchive` method to use `input.getOwnerId()` instead of `input.getUserId()`
- ownerId is set from the input (Tenant ID)
- userId is still used to assign the creator as OWNER role

---

## Archive-Tenant Association

### Database Schema
```
archives table:
  - id (PK)
  - owner_id (FK → tenants.id) ← Tenant association
  - title
  - description
  - content
  - created_at
  - updated_at
  - status
  - standard
```

### Relationship
```
Tenant (id) ←→ Archive (owner_id)
  One tenant can own many archives
  Each archive belongs to one tenant
```

---

## Access Control

### Tenant Archive Create Page Safeguard
```typescript
// ADMIN can create for any tenant, TENANT for their own
if (currentRole === 'ADMIN') {
  hasAccess = true;
} else if (currentRole === 'TENANT' && tenantId === data.tenantId) {
  hasAccess = true;
} else if (currentRole === 'USER') {
  hasAccess = false;
  goto('/');  // USER cannot create archives
} else {
  goto('/login');
}
```

### Access Matrix

| Route | ADMIN | TENANT | USER |
|-------|-------|--------|------|
| `/tenants/{id}/create` | ✅ Any tenant | ✅ Own tenant | ⛔ → Home |
| `/tenants/{id}/archives` | ✅ View any | ✅ View own | ⛔ Redirect |

---

## Features

The new `/tenants/[id]/create` page includes:

### UI Features
- ✅ Tenant name badge showing which tenant the archive is for
- ✅ Access denied screen for unauthorized users
- ✅ Breadcrumb navigation back to archives
- ✅ Clean, focused form layout
- ✅ Field hints for better UX
- ✅ Info box showing tenant association
- ✅ Loading and error states
- ✅ Form validation

### Form Fields
- **Creator/User** * (required) - Select from users
- **Title** * (required) - Archive title
- **Description** (optional) - Archive description
- **Content** * (required) - Archive content/metadata
- **Standard** * (required) - NOARK5, OAIS, PREMIS, Dublin Core, METS, EAD, BagIt, ISAD(G), MODS

### Automatic Association
- **ownerId** is automatically set to the tenant ID from the route
- Archives are created under the tenant's ownership
- Creator is assigned as OWNER role
- Clear indication of tenant association in UI

---

## Navigation Flow

### ADMIN Creating Archive for Tenant
```
1. Navigate to /tenants/1/archives
   ↓
2. Click "+ Create Archive" button
   ↓
3. Goes to: /tenants/1/create
   ↓
4. Fill in form (ownerId = 1 automatically)
   ↓
5. Click "Create Archive"
   ↓
6. Archive created with ownerId = 1
   ↓
7. Redirects to: /tenants/1/archives
   ↓
8. New archive appears in tenant's list
```

### TENANT Creating Archive for Their Tenant
```
1. Navigate to /tenants/1/archives (their tenant)
   ↓
2. Click "+ Create Archive" button
   ↓
3. Goes to: /tenants/1/create
   ↓
4. Fill in form (ownerId = 1 automatically)
   ↓
5. Create archive
   ↓
6. Archive owned by their tenant
```

### USER Trying to Create Archive
```
1. Try to access /tenants/1/create
   ↓
2. Security guard blocks access
   ↓
3. Redirects to: /
   ↓
4. Cannot create archives ✅
```

---

## GraphQL Mutation

### CreateArchive with ownerId
```graphql
mutation CreateArchive($input: CreateArchiveInput!) {
  createArchive(input: $input) {
    id
    ownerId      ← Tenant ID
    title
    description
    content
    createdAt
    updatedAt
    status
    standard
  }
}
```

### Variables
```json
{
  "input": {
    "ownerId": 1,        // Tenant ID
    "userId": 3,         // Creator user ID
    "title": "Q1 Report",
    "description": "Financial report",
    "content": "Archive content...",
    "standard": "NOARK5"
  }
}
```

---

## Backend Logic

### Archive Creation Flow
```java
public Archive createArchive(CreateArchiveInput input) {
    // Validate user exists
    if (!userApi.userExists(input.getUserId())) {
        throw new IllegalArgumentException("User does not exist");
    }

    Archive archive = new Archive(
        null,
        input.getOwnerId(),  // ← Tenant ID from input
        input.getTitle(),
        input.getDescription(),
        input.getContent(),
        now,
        now,
        ArchiveStatus.DRAFT,
        input.getStandard()
    );

    // Assign creator as OWNER
    archive.assignUser(input.getUserId(), UserRole.OWNER);

    return archiveRepository.save(archive);
}
```

---

## Benefits

### Clear Ownership
✅ Archives are explicitly owned by tenants
✅ ownerId field clearly indicates tenant association
✅ Easy to query archives by tenant

### Better Organization
✅ Tenant-scoped archive creation
✅ Archives organized under tenants
✅ Clear hierarchy: Tenant → Archives

### Improved Security
✅ TENANT can only create archives for their own tenant
✅ ADMIN can create for any tenant
✅ USER cannot create archives at all
✅ Proper safeguards at page level

### Better UX
✅ Tenant badge shows context
✅ Automatic tenant association
✅ Clear indication of which tenant owns the archive
✅ Simplified workflow

---

## Complete Tenant Archive Structure

```
/tenants/{id}/
  ├─ +page.svelte          → Tenant detail
  ├─ archives/
  │   └─ +page.svelte      → List tenant archives
  ├─ create/
  │   └─ +page.svelte      → Create archive for tenant ✨ NEW
  ├─ users/                → Tenant users
  └─ documents/            → Tenant documents
```

---

## Files Created/Modified

### Frontend
1. ✅ **Created**: `/frontend/src/routes/tenants/[id]/create/+page.ts` (NEW)
   - Page load function to extract tenantId

2. ✅ **Created**: `/frontend/src/routes/tenants/[id]/create/+page.svelte` (NEW - 500+ lines)
   - Tenant-scoped archive creation page
   - Enhanced with safeguards and tenant association
   - Automatic ownerId = tenantId

3. ✅ `/frontend/src/routes/tenants/[id]/archives/+page.svelte`
   - Updated "+ Create Archive" button to `/tenants/{id}/create`

### Backend
4. ✅ `/src/main/java/com/dmc/archiving/archive/input/CreateArchiveInput.java`
   - Added `ownerId` field (required)
   - Now requires both ownerId (tenant) and userId (creator)

5. ✅ `/src/main/java/com/dmc/archiving/archive/ArchiveService.java`
   - Updated `createArchive` to use `input.getOwnerId()`
   - Archives now properly associated with tenant

---

## Testing

### Test ADMIN Creating Archive

1. **Login as ADMIN**
   ```
   Username: admin
   Password: admin123
   ```

2. **Navigate to tenant archives**
   - Go to: `/tenants/1/archives`
   - Click "+ Create Archive"
   - Should go to: `/tenants/1/create`

3. **Create archive**
   - See tenant badge: "🏢 Acme Corporation"
   - Fill in form:
     - Creator: Select a user
     - Title: "Test Archive"
     - Content: "Test content"
     - Standard: NOARK5
   - Click "Create Archive"
   - Should redirect to: `/tenants/1/archives`
   - New archive should appear with ownerId = 1

4. **Verify in database**
   ```sql
   SELECT id, owner_id, title FROM archives WHERE owner_id = 1;
   ```
   Should show new archive with owner_id = 1 ✅

### Test TENANT Creating Archive

1. **Login as TENANT**
   ```
   Username: tenant
   Password: tenant123
   ```

2. **Create archive for own tenant**
   - Go to: `/tenants/1/archives` (assuming tenantId = 1)
   - Click "+ Create Archive"
   - Should go to: `/tenants/1/create`
   - Fill form and create
   - Archive owned by tenant 1 ✅

3. **Try to create for another tenant**
   - Try: `/tenants/2/create`
   - Should redirect (no access) ✅

### Test USER Access

1. **Login as USER**
   ```
   Username: user
   Password: user123
   ```

2. **Try accessing create page**
   - Navigate to: `/tenants/1/create`
   - Should redirect to: `/` ✅
   - Cannot create archives

---

## Query Archives by Tenant

Now that archives have ownerId, you can easily query by tenant:

### GraphQL Query
```graphql
query GetArchivesByOwner($ownerId: ID!) {
  getArchivesByOwner(ownerId: $ownerId) {
    id
    ownerId
    title
    description
    status
    standard
    createdAt
  }
}
```

### SQL Query
```sql
SELECT * FROM archives 
WHERE owner_id = 1 
ORDER BY created_at DESC;
```

---

## Data Model

### Before (Implicit)
```
Archive:
  - userId (creator, but not owner)
  - No explicit tenant association
```

### After (Explicit)
```
Archive:
  - ownerId (Tenant ID) ← Explicit owner
  - userId (creator, assigned as OWNER role)
  - Clear tenant-archive relationship
```

---

## Status: ✅ COMPLETE

Tenant-scoped archive creation successfully implemented at `/tenants/[id]/create` with:
- ✅ Automatic tenant association (ownerId = tenantId)
- ✅ Enhanced access control (ADMIN/TENANT only)
- ✅ Backend updated to support ownerId
- ✅ UI shows tenant context clearly
- ✅ Proper redirects after creation
- ✅ Archives explicitly owned by tenants

**Ready for testing!** 🚀

**Archives are now properly associated with tenants!** 🎉

