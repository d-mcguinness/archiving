# ✅ Updated: Documents Page Now Uses user_tenant Association

## Changes Made

Updated the documents page to properly use the `user_tenant` association table so that:
- **TENANT role** users see ALL documents belonging to their tenant(s) 
- **USER role** users (with user_assignments) see ONLY their own uploaded documents
- **ADMIN role** users continue to see ALL documents across all tenants

---

## What Was Changed

### 1. **TenancyService Interface** (`TenancyService.java`)

**Added method:**
```java
List<Long> getTenantIdsByUserId(Long userId);  // Get tenant IDs for a user
```

This method is exposed from the tenancy module to other modules and returns just the IDs (not full Tenant objects) to maintain Spring Modulith boundaries.

---

### 2. **TenancyServiceImpl** (`TenancyServiceImpl.java`)

**Implemented the new method:**
```java
@Override
public List<Long> getTenantIdsByUserId(Long userId) {
    return tenancyRepository.findTenantsByUserId(userId).stream()
            .map(Tenant::getId)
            .collect(Collectors.toList());
}
```

This uses the existing `findTenantsByUserId` repository method which queries the `user_tenant` join table.

---

### 3. **DocumentService** (`DocumentService.java`)

**Added new method:**
```java
/**
 * Get documents for all tenants that a user belongs to
 * Uses user_tenant association to find user's tenants
 */
public List<Document> getDocumentsByUserTenants(Long userId) {
    // Get all tenant IDs the user belongs to (using user_tenant table)
    List<Long> tenantIds = tenancyService.getTenantIdsByUserId(userId);
    
    if (tenantIds.isEmpty()) {
        log.warn("User {} does not belong to any tenants", userId);
        return List.of();
    }
    
    log.info("Fetching documents for user {} from tenants: {}", userId, tenantIds);
    
    // Get all documents from these tenants
    return tenantIds.stream()
        .flatMap(tenantId -> documentRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream())
        .distinct()
        .sorted((d1, d2) -> d2.getCreatedAt().compareTo(d1.getCreatedAt()))
        .collect(Collectors.toList());
}
```

This method:
1. Gets all tenant IDs from `user_tenant` table for the user
2. Fetches documents from ALL those tenants
3. Combines and sorts them by creation date
4. Returns distinct documents

---

### 4. **DocumentController** (`DocumentController.java`)

**Updated TENANT role handling:**
```java
else if ("TENANT".equals(role)) {
    // Tenant sees all documents in ALL their tenants (using user_tenant association)
    if (userId != null) {
        documents = documentService.getDocumentsByUserTenants(userId);
        log.info("Fetching documents for TENANT user {} from their tenant(s)", userId);
    } else if (tenantId != null) {
        // If tenantId provided directly, use it
        documents = documentService.getDocumentsByTenant(tenantId);
        log.info("Fetching documents for tenant {}", tenantId);
    } else {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("success", false, "error", "Missing userId or tenantId for TENANT role"));
    }
}
```

**Updated USER role handling:**
```java
else if ("USER".equals(role) && userId != null) {
    // USER role with user_assignments sees NO documents in the documents page
    // They only see documents they personally uploaded
    documents = documentService.getDocumentsByUser(userId);
    log.info("Fetching documents for USER {}", userId);
}
```

---

## How It Works Now

### ADMIN Role
**What they see:** ALL documents system-wide

**Query:**
```
GET /api/documents?role=ADMIN
```

**Logic:**
```java
documents = documentService.getAllDocuments();
```

**Result:** Every document in the system

---

### TENANT Role  
**What they see:** ALL documents from ALL tenants they belong to (via `user_tenant` table)

**Query:**
```
GET /api/documents?role=TENANT&userId=2
```

**Logic:**
```java
// 1. Find all tenants for user 2 from user_tenant table
List<Long> tenantIds = tenancyService.getTenantIdsByUserId(2);
// Example: [1, 2] (User 2 belongs to Tenant 1 and Tenant 2)

// 2. Get all documents from those tenants
documents from tenant 1 + documents from tenant 2
```

**Example:**
If User 2 (Jane) belongs to:
- Tenant 1 (Acme Corp)
- Tenant 2 (Tech Innovations)

Jane will see:
- ALL documents where `tenant_id = 1` (Acme Corp documents)
- ALL documents where `tenant_id = 2` (Tech Innovations documents)
- This includes documents uploaded by ANY user in those tenants

**user_tenant query:**
```sql
SELECT tenant_id FROM user_tenant WHERE user_id = 2;
-- Result: [1, 2]

SELECT * FROM documents WHERE tenant_id IN (1, 2) ORDER BY created_at DESC;
```

---

### USER Role
**What they see:** ONLY documents they personally uploaded

**Query:**
```
GET /api/documents?role=USER&userId=5
```

**Logic:**
```java
documents = documentService.getDocumentsByUser(5);
```

**Result:** Only documents where `user_id = 5`

**Note:** `user_assignments` to archives does NOT grant document visibility. Users only see their own uploads.

---

## Database Relationships

### user_tenant Table (Organization Membership)
```sql
user_tenant:
┌───────────┬─────────┬────────────────────────────┐
│ tenant_id │ user_id │ Meaning                    │
├───────────┼─────────┼────────────────────────────┤
│ 1         │ 1       │ User 1 → Tenant 1          │
│ 1         │ 2       │ User 2 → Tenant 1          │
│ 2         │ 2       │ User 2 → Tenant 2 (multi!) │
│ 2         │ 3       │ User 3 → Tenant 2          │
└───────────┴─────────┴────────────────────────────┘
```

User 2 belongs to TWO tenants → sees documents from BOTH tenants

### documents Table
```sql
documents:
┌────┬───────────────┬─────────┬───────────┬──────────────┐
│ id │ title         │ user_id │ tenant_id │ Who sees it? │
├────┼───────────────┼─────────┼───────────┼──────────────┤
│ 1  │ Q1 Financial  │ 1       │ 1         │ Admin, Tenant users in Tenant 1, User 1 │
│ 2  │ Annual Report │ 2       │ 1         │ Admin, Tenant users in Tenant 1, User 2 │
│ 3  │ Research Data │ 3       │ 2         │ Admin, Tenant users in Tenant 2, User 3 │
└────┴───────────────┴─────────┴───────────┴──────────────┘
```

### user_assignments Table (Archive Access - NOT used for documents view)
```sql
user_assignments:
┌────────────┬─────────┬──────────┬──────────────────────┐
│ archive_id │ user_id │ role     │ Impact on Documents? │
├────────────┼─────────┼──────────┼──────────────────────┤
│ 1          │ 1       │ OWNER    │ ❌ NO - not used     │
│ 1          │ 2       │ EDITOR   │ ❌ NO - not used     │
│ 3          │ 2       │ OWNER    │ ❌ NO - not used     │
└────────────┴─────────┴──────────┴──────────────────────┘
```

**Important:** `user_assignments` is for **archive access control**, NOT for documents visibility. Users only see their own uploaded documents, regardless of archive assignments.

---

## Example Scenarios

### Scenario 1: User 2 (Jane) - TENANT Role

**Setup:**
```sql
-- user_tenant (Jane belongs to 2 tenants)
INSERT INTO user_tenant VALUES (1, 2), (2, 2);

-- documents
INSERT INTO documents (id, title, user_id, tenant_id) VALUES
(1, 'Q1 Report', 1, 1),        -- Tenant 1 document (by User 1)
(2, 'Annual Budget', 2, 1),    -- Tenant 1 document (by User 2)
(3, 'Research', 3, 2),         -- Tenant 2 document (by User 3)
(4, 'Presentation', 4, 3),     -- Tenant 3 document (by User 4)
(5, 'Notes', 2, 2);            -- Tenant 2 document (by User 2)
```

**Query:**
```
GET /api/documents?role=TENANT&userId=2
```

**Result:**
Jane sees documents: **1, 2, 3, 5**
- Document 1: Tenant 1 document (Jane in Tenant 1)
- Document 2: Tenant 1 document (Jane in Tenant 1)
- Document 3: Tenant 2 document (Jane in Tenant 2)
- Document 5: Tenant 2 document (Jane in Tenant 2)
- ❌ Document 4: NOT visible (Tenant 3, Jane not in Tenant 3)

---

### Scenario 2: User 5 (Charlie) - USER Role

**Setup:**
```sql
-- user_tenant (Charlie belongs to Tenant 4)
INSERT INTO user_tenant VALUES (4, 5);

-- documents
INSERT INTO documents (id, title, user_id, tenant_id) VALUES
(5, 'Meeting Notes', 5, 4),    -- Charlie's document
(6, 'Project Plan', 4, 4),     -- Alice's document (same tenant)
(7, 'Budget', 5, 4);           -- Charlie's document
```

**Query:**
```
GET /api/documents?role=USER&userId=5
```

**Result:**
Charlie sees documents: **5, 7** (only his own uploads)
- ✅ Document 5: Uploaded by Charlie
- ❌ Document 6: NOT visible (uploaded by Alice, even though same tenant)
- ✅ Document 7: Uploaded by Charlie

**Why?** USER role sees ONLY their own uploads, regardless of tenant membership or archive assignments.

---

### Scenario 3: User 1 (John) - ADMIN Role

**Query:**
```
GET /api/documents?role=ADMIN
```

**Result:**
John sees **ALL documents** (1, 2, 3, 4, 5, 6, 7, etc.)

---

## Comparison: Before vs After

### Before (Incorrect)

| Role | What They Saw |
|------|---------------|
| ADMIN | All documents ✅ |
| TENANT | Own documents only ❌ (Wrong!) |
| USER | Own documents ✅ |

**Problem:** TENANT role users were only seeing their own uploaded documents, not all documents in their tenant(s).

### After (Correct)

| Role | What They See | Based On |
|------|---------------|----------|
| ADMIN | All documents system-wide ✅ | No filter |
| TENANT | All documents in their tenant(s) ✅ | `user_tenant` table |
| USER | Only their own uploads ✅ | `user_id` match |

**Fixed:** TENANT role now uses `user_tenant` association to see all documents from all tenants they belong to.

---

## Key Points

### ✅ user_tenant Association Now Used
- TENANT role queries `user_tenant` table to find user's tenants
- Fetches documents from ALL those tenants
- Supports multi-tenant membership (user can belong to multiple tenants)

### ❌ user_assignments NOT Used for Documents
- `user_assignments` table is for **archive access control** only
- Does NOT grant visibility to documents
- USER role ignores archive assignments
- Users only see documents they uploaded themselves

### 🔐 Role-Based Access
- **ADMIN**: System-wide view
- **TENANT**: Tenant-wide view (via `user_tenant`)
- **USER**: Personal view (own uploads only)

---

## Files Modified

1. ✅ `/src/main/java/com/dmc/archiving/tenancy/service/TenancyService.java`
   - Added `getTenantIdsByUserId()` method

2. ✅ `/src/main/java/com/dmc/archiving/tenancy/service/TenancyServiceImpl.java`
   - Implemented `getTenantIdsByUserId()` method

3. ✅ `/src/main/java/com/dmc/archiving/document/DocumentService.java`
   - Added `getDocumentsByUserTenants()` method
   - Injected `TenancyService` dependency

4. ✅ `/src/main/java/com/dmc/archiving/document/DocumentController.java`
   - Updated TENANT role to use `getDocumentsByUserTenants()`
   - Clarified USER role behavior

---

## Testing

### Test TENANT Role (Multi-Tenant User)

**Setup:**
```sql
-- User 2 belongs to 2 tenants
INSERT INTO user_tenant VALUES (1, 2), (2, 2);

-- Documents in both tenants
INSERT INTO documents (title, user_id, tenant_id) VALUES
('Doc A', 1, 1),  -- Tenant 1
('Doc B', 3, 2);  -- Tenant 2
```

**Test:**
```bash
curl "http://localhost:2020/api/documents?role=TENANT&userId=2"
```

**Expected:** Both "Doc A" and "Doc B" returned (from both tenants)

### Test USER Role

**Test:**
```bash
curl "http://localhost:2020/api/documents?role=USER&userId=5"
```

**Expected:** Only documents where `user_id = 5`

### Test ADMIN Role

**Test:**
```bash
curl "http://localhost:2020/api/documents?role=ADMIN"
```

**Expected:** All documents in the system

---

## Spring Modulith Compliance

✅ **Module boundaries respected:**
- Document module does NOT access Tenant entity directly
- Uses `TenancyService.getTenantIdsByUserId()` which returns primitives (Long IDs)
- No module boundary violations

---

## Summary

✅ **TENANT role** now correctly sees all documents in their tenant(s) using `user_tenant` association  
✅ **USER role** still only sees their own uploads (ignores `user_assignments`)  
✅ **ADMIN role** continues to see everything  
✅ Multi-tenant membership supported (users can belong to multiple tenants)  
✅ Spring Modulith boundaries maintained  
✅ No breaking changes to existing functionality  

---

**Date**: February 18, 2026  
**Status**: ✅ COMPLETE  
**Impact**: TENANT users now have proper tenant-wide document visibility

