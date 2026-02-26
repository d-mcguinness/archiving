# Dashboard Comparison: ADMIN vs TENANT

## Visual Comparison

### ADMIN Dashboard
```
╔═══════════════════════════════════════════════════════════╗
║ Dashboard                                                 ║
╠═══════════════════════════════════════════════════════════╣
║                                                           ║
║  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ║
║  │   👥 Users   │  │ 🏢 Tenants   │  │ 📁 Archives  │  ║
║  │              │  │              │  │              │  ║
║  │     150      │  │      12      │  │     450      │  ║
║  │              │  │              │  │              │  ║
║  │ Manage Users │  │Manage Tenants│  │Manage Archives│  ║
║  └──────────────┘  └──────────────┘  └──────────────┘  ║
║                                                           ║
║  Archive Breakdown                                        ║
║  ┌──────────────────────────────────────────────────┐   ║
║  │ ● Active: 350    ● Draft: 80    ● Archived: 20  │   ║
║  └──────────────────────────────────────────────────┘   ║
║                                                           ║
║  Quick Actions                                            ║
║  [Create User]  [Create Tenant]  [Create Archive]        ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
```

**Data Source**: `getDashboardStats()`
- **Scope**: System-wide (all tenants combined)
- **Users**: All users in the system
- **Tenants**: Total tenant count
- **Archives**: All archives owned by all tenants

---

### TENANT Dashboard
```
╔═══════════════════════════════════════════════════════════╗
║ Dashboard                                                 ║
╠═══════════════════════════════════════════════════════════╣
║  ┌─────────────────────────────────────────────────────┐ ║
║  │ 🏢 Tech Innovations                                 │ ║
║  │ [ACTIVE] [PROFESSIONAL]                             │ ║
║  └─────────────────────────────────────────────────────┘ ║
║                                                           ║
║  ┌──────────────┐  ┌──────────────┐                     ║
║  │   👥 Users   │  │ 📁 Archives  │                     ║
║  │              │  │              │                     ║
║  │      15      │  │      45      │                     ║
║  │              │  │              │                     ║
║  │ Manage Users │  │Manage Archives│                     ║
║  └──────────────┘  └──────────────┘                     ║
║                                                           ║
║  Archive Breakdown                                        ║
║  ┌──────────────────────────────────────────────────┐   ║
║  │ ● Active: 30     ● Draft: 10    ● Archived: 5   │   ║
║  └──────────────────────────────────────────────────┘   ║
║                                                           ║
║  Quick Actions                                            ║
║  [Create User]  [Create Archive]                          ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
```

**Data Source**: `getTenantDashboardStats(tenantId: 2)`
- **Scope**: Single tenant only (tenant ID 2)
- **Users**: Only users in this tenant (from `user_tenant` table)
- **Tenants**: Not shown (not relevant for tenant users)
- **Archives**: Only archives owned by this tenant (`owner_id = 2`)

---

## Key Differences

| Feature | ADMIN | TENANT |
|---------|-------|--------|
| **Tenant Banner** | ❌ No | ✅ Yes (shows tenant name, status, plan) |
| **Users Card** | ✅ All users | ✅ Users in tenant only |
| **Tenants Card** | ✅ Total tenants | ❌ Not shown |
| **Archives Card** | ✅ All archives | ✅ Archives owned by tenant |
| **Data Scope** | System-wide | Tenant-specific |
| **GraphQL Query** | `getDashboardStats` | `getTenantDashboardStats(tenantId)` |

---

## Example Scenarios

### Scenario 1: Acme Corp (Tenant ID 1)

**ADMIN sees**:
- Users: 150 (all users across all tenants)
- Tenants: 12
- Archives: 450 (all archives from all tenants)

**TENANT (Acme Corp) sees**:
```
🏢 Acme Corporation
[ACTIVE] [ENTERPRISE]

Users: 25 (only Acme Corp users)
Archives: 120 (only Acme Corp archives)
```

---

### Scenario 2: Tech Innovations (Tenant ID 2)

**ADMIN sees**:
- Users: 150 (same system-wide total)
- Tenants: 12
- Archives: 450 (same system-wide total)

**TENANT (Tech Innovations) sees**:
```
🏢 Tech Innovations
[ACTIVE] [PROFESSIONAL]

Users: 15 (only Tech Innovations users)
Archives: 45 (only Tech Innovations archives)
```

---

### Scenario 3: Global Solutions (Tenant ID 3)

**ADMIN sees**:
- Users: 150 (same system-wide total)
- Tenants: 12
- Archives: 450 (same system-wide total)

**TENANT (Global Solutions) sees**:
```
🏢 Global Solutions Inc.
[TRIAL] [BASIC]

Users: 8 (only Global Solutions users)
Archives: 22 (only Global Solutions archives)
```

---

## Data Isolation

### ADMIN Query
```sql
-- Gets everything
SELECT COUNT(*) FROM users;           -- 150
SELECT COUNT(*) FROM tenants;         -- 12
SELECT COUNT(*) FROM archives;        -- 450
```

### TENANT Query (Tenant ID 2)
```sql
-- Gets tenant-specific data
SELECT name, status, plan FROM tenants WHERE id = 2;

-- Users in tenant (from join table)
SELECT COUNT(*) 
FROM user_tenant 
WHERE tenant_id = 2;                  -- 15

-- Archives owned by tenant
SELECT COUNT(*) 
FROM archives 
WHERE owner_id = 2;                   -- 45
```

---

## Security Benefits

### ADMIN
✅ Can see all data across all tenants
✅ System-wide visibility for administration
✅ Can compare tenant metrics

### TENANT
✅ Cannot see other tenants' data
✅ Only sees their own users and archives
✅ Data isolation enforced at backend
✅ No way to query other tenants' information

---

## UI Elements

### Tenant Info Banner (TENANT only)
```css
.tenant-info-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 2rem;
  border-radius: 0.75rem;
}
```

**Shows**:
- 🏢 Tenant name (display name or name)
- Status badge (ACTIVE, TRIAL, SUSPENDED, etc.)
- Plan badge (FREE, BASIC, PROFESSIONAL, ENTERPRISE)

### Stats Cards
- **ADMIN**: 3 cards (Users, Tenants, Archives)
- **TENANT**: 2 cards (Users, Archives)

### Archive Breakdown
- Both roles see breakdown by status
- ADMIN: breakdown across all tenants
- TENANT: breakdown for their tenant only

---

## Implementation Details

### Backend Logic
```java
// ADMIN Dashboard
@QueryMapping
public DashboardStats getDashboardStats() {
    stats.setTotalUsers(userService.getAllUsers().size());
    stats.setTotalTenants(tenancyService.getAllTenants().size());
    stats.setTotalArchives(archiveService.getAllArchives().size());
    return stats;
}

// TENANT Dashboard
@QueryMapping
public TenantDashboardStats getTenantDashboardStats(@Argument Long tenantId) {
    Tenant tenant = tenancyService.getTenantById(tenantId);
    stats.setTenantName(tenant.getDisplayName());
    stats.setTotalUsers(tenant.getUsers().size());  // From user_tenant
    
    List<Archive> archives = archiveService.getArchivesByOwner(tenantId);
    stats.setTotalArchives(archives.size());
    return stats;
}
```

### Frontend Logic
```typescript
// ADMIN
if (currentRole === 'ADMIN') {
  await loadAdminDashboardStats();  // Uses GET_DASHBOARD_STATS
}

// TENANT
if (currentRole === 'TENANT' && currentTenantId) {
  await loadTenantDashboardStats(currentTenantId);  // Uses GET_TENANT_DASHBOARD_STATS
}
```

---

## Summary

| Aspect | ADMIN Dashboard | TENANT Dashboard |
|--------|----------------|------------------|
| **Purpose** | System administration | Tenant management |
| **Scope** | All tenants | Single tenant |
| **Visibility** | System-wide | Tenant-scoped |
| **Users shown** | All | Tenant members only |
| **Archives shown** | All | Tenant's archives only |
| **Tenant info** | Count only | Detailed (name, status, plan) |
| **Security** | Full access | Isolated data |

Both dashboards provide the appropriate level of detail and control for their respective roles!

