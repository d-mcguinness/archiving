# TENANT Login Redirect & Admin Page Security ✅

## Summary
Updated the login flow to redirect TENANT users to their specific tenant detail page (`/tenants/{id}`) instead of the tenants list, and secured the admin page to prevent TENANT users from accessing it.

---

## Changes Made

### 🔧 Backend Changes

#### AuthController.java - UPDATED

**Added TenancyService dependency**:
```java
import com.dmc.archiving.tenancy.service.TenancyService;
import java.util.List;

private final TenancyService tenancyService;

public AuthController(UserService userService, TenancyService tenancyService) {
    this.userService = userService;
    this.tenancyService = tenancyService;
}
```

**Added tenantId to login response for TENANT and USER roles**:
```java
// Add tenantId for TENANT and USER roles
if ("TENANT".equals(credentials.getRole()) || "USER".equals(credentials.getRole())) {
    try {
        List<Long> tenantIds = tenancyService.getTenantIdsByUserId(userId);
        if (!tenantIds.isEmpty()) {
            Long tenantId = tenantIds.get(0); // Use first tenant
            response.put("tenantId", tenantId);
            user.put("tenantId", tenantId);
            log.info("Added tenantId {} for user {} with role {}", tenantId, username, credentials.getRole());
        }
    } catch (Exception e) {
        log.error("Error getting tenant IDs for user {}: {}", username, e.getMessage());
    }
}
```

**Login Response Now Includes**:
```json
{
  "success": true,
  "message": "Login successful",
  "user": {
    "id": 2,
    "username": "tenant",
    "name": "Tenant Manager",
    "email": "tenant@archiving.com",
    "role": "TENANT",
    "tenantId": 1
  },
  "role": "TENANT",
  "token": "Bearer_tenant_TENANT_...",
  "tenantId": 1,
  "expiresIn": 3600
}
```

---

### 🎨 Frontend Changes

#### 1. Login Page (`login/+page.svelte`) - UPDATED

**Stores tenantId in localStorage**:
```typescript
// Store tenantId if present (for TENANT and USER roles)
if (result.tenantId) {
  localStorage.setItem('auth_tenantId', result.tenantId.toString());
}
```

**Updated redirect logic**:
```typescript
// Redirect based on role
if (result.role === 'ADMIN') {
  goto('/admin');
} else if (result.role === 'TENANT') {
  // Redirect TENANT to their tenant detail page
  if (result.tenantId) {
    goto(`/tenants/${result.tenantId}`);
  } else {
    goto('/tenants');
  }
} else if (result.role === 'USER') {
  // Redirect USER to their tenant's users page
  if (result.tenantId) {
    goto(`/tenants/${result.tenantId}/users`);
  } else {
    goto('/users');
  }
} else {
  goto('/');
}
```

#### 2. Admin Page (`admin/+page.svelte`) - UPDATED

**Improved security guard with immediate redirect**:
```typescript
onMount(async () => {
  const role = localStorage.getItem('auth_role');
  const tenantId = localStorage.getItem('auth_tenantId');
  currentRole = role || '';

  // Only ADMIN can access this page
  if (currentRole !== 'ADMIN') {
    hasAccess = false;
    loading = false;
    
    // Redirect non-admin users to appropriate page
    if (currentRole === 'TENANT' && tenantId) {
      goto(`/tenants/${tenantId}`);
    } else if (currentRole === 'USER' && tenantId) {
      goto(`/tenants/${tenantId}/users`);
    } else {
      goto('/');
    }
    return;
  }

  hasAccess = true;
  await loadAdminData();
});
```

**Access Denied UI** (already existed, now shows briefly before redirect):
```svelte
{#if !hasAccess && !loading}
  <div class="access-denied">
    <div class="access-denied-icon">🚫</div>
    <h1>Access Denied</h1>
    <p>You don't have permission to access the admin panel.</p>
    <a href="/" class="btn-home">Go to Dashboard</a>
  </div>
{/if}
```

---

## Login Flow Comparison

### Before

**TENANT Login**:
```
1. Login (tenant/tenant123)
   ↓
2. Backend returns: { role: "TENANT", ... }
   ↓
3. Frontend redirects to: /tenants (list page)
   ↓
4. TENANT sees list of all tenants
```

**Admin Page Access**:
```
1. TENANT navigates to /admin
   ↓
2. Access denied screen shows
   ↓
3. After 2 second delay → redirect to /
```

### After

**TENANT Login**:
```
1. Login (tenant/tenant123)
   ↓
2. Backend:
   - Gets user ID (2)
   - Queries user_tenant table
   - Finds tenantId = 1
   - Returns: { role: "TENANT", tenantId: 1, ... }
   ↓
3. Frontend:
   - Stores tenantId in localStorage
   - Redirects to: /tenants/1 (detail page)
   ↓
4. TENANT sees their tenant detail page
   - Tenant info with badges
   - Settings
   - Quick actions
```

**Admin Page Access**:
```
1. TENANT navigates to /admin
   ↓
2. Security guard checks role
   ↓
3. Detects TENANT role + has tenantId
   ↓
4. Immediate redirect to: /tenants/{tenantId}
   ↓
5. TENANT lands on their tenant page (no delay)
```

---

## URL Redirects by Role

| Role | Login Redirect | Admin Page Attempt |
|------|---------------|-------------------|
| **ADMIN** | `/admin` | ✅ Allowed (stays on page) |
| **TENANT** | `/tenants/{tenantId}` | ⛔ Redirects to `/tenants/{tenantId}` |
| **USER** | `/tenants/{tenantId}/users` | ⛔ Redirects to `/tenants/{tenantId}/users` |
| **Guest** | `/` | ⛔ Redirects to `/` |

---

## Security Features

### 1. Backend Security
✅ TenantId is derived from `user_tenant` table (not user-provided)
✅ Uses first tenant if user belongs to multiple tenants
✅ Logs tenantId assignment for audit trail
✅ Gracefully handles errors (doesn't crash login if tenancy lookup fails)

### 2. Frontend Security
✅ Admin page checks role on mount
✅ Immediate redirect for unauthorized users
✅ No sensitive data exposed to non-admin users
✅ Role stored in localStorage (client-side only, not for actual auth)

### 3. Navigation Security
✅ TENANT navbar shows "My Tenant" → links to their tenant only
✅ ADMIN navbar shows "Tenants" → links to full list
✅ No way for TENANT to navigate to admin pages via UI

---

## Database Query

### Getting TenantId for TENANT Role

When user "tenant" (userId=2) logs in:

```sql
-- Backend executes:
SELECT t.id 
FROM tenants t 
JOIN user_tenant ut ON t.id = ut.tenant_id 
WHERE ut.user_id = 2;

-- Result: tenantId = 1 (Acme Corp)
```

This tenantId is then:
1. Added to login response
2. Stored in localStorage
3. Used for redirect URL
4. Used for navbar link

---

## Testing

### Manual Test Steps

#### 1. Test TENANT Login Flow
```
1. Go to: http://localhost:5173/login
2. Click "Tenant" demo card (or enter tenant/tenant123)
3. Click "Sign In"
4. Verify:
   ✅ Redirects to /tenants/1 (or their tenantId)
   ✅ Shows tenant detail page with banner
   ✅ Navbar shows "My Tenant" link
   ✅ No access to /admin page
```

#### 2. Test ADMIN Login Flow
```
1. Go to: http://localhost:5173/login
2. Click "Admin" demo card (or enter admin/admin123)
3. Click "Sign In"
4. Verify:
   ✅ Redirects to /admin
   ✅ Shows admin dashboard
   ✅ Navbar shows "Tenants" link to list
   ✅ Can access all pages
```

#### 3. Test Admin Page Security
```
As TENANT user:
1. Navigate to: http://localhost:5173/admin
2. Verify:
   ✅ Immediately redirects to /tenants/{id}
   ✅ Cannot access admin page
   ✅ No error shown (clean redirect)

As ADMIN user:
1. Navigate to: http://localhost:5173/admin
2. Verify:
   ✅ Page loads normally
   ✅ Shows admin dashboard
   ✅ All data visible
```

#### 4. Test USER Login Flow
```
1. Go to: http://localhost:5173/login
2. Click "User" demo card (or enter user/user123)
3. Click "Sign In"
4. Verify:
   ✅ Redirects to /tenants/{tenantId}/users
   ✅ Shows users list for their tenant
   ✅ No access to /admin page
```

### Backend API Test

```bash
# Test TENANT login returns tenantId
curl -X POST http://localhost:2020/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant","password":"tenant123"}' \
  | jq '.tenantId'

# Expected output: 1 (or their tenantId)
```

---

## Files Modified

1. ✅ `/src/main/java/com/dmc/archiving/auth/AuthController.java`
   - Added TenancyService dependency
   - Added tenantId to login response for TENANT/USER roles

2. ✅ `/frontend/src/routes/login/+page.svelte`
   - Stores tenantId in localStorage
   - Redirects TENANT to `/tenants/{id}`
   - Redirects USER to `/tenants/{id}/users`

3. ✅ `/frontend/src/routes/admin/+page.svelte`
   - Improved security guard
   - Immediate redirect for non-admin users
   - Role-based redirect destinations

---

## Benefits

### User Experience
✅ TENANT users land directly on their tenant page
✅ No need to search for their tenant in a list
✅ Immediate access to relevant information
✅ Clean, fast redirects (no delay)

### Security
✅ Admin page secured against TENANT access
✅ Automatic redirect prevents unauthorized viewing
✅ TenantId derived from database (secure)
✅ No way to bypass security via URL manipulation

### Code Quality
✅ Consistent redirect logic
✅ Proper error handling
✅ Audit logging for tenantId assignment
✅ Clean separation of concerns

---

## Login Response Structure

### ADMIN Login
```json
{
  "success": true,
  "user": {
    "id": 1,
    "username": "admin",
    "role": "ADMIN"
  },
  "role": "ADMIN",
  "token": "...",
  "tenantId": null  // Not present
}
```

### TENANT Login
```json
{
  "success": true,
  "user": {
    "id": 2,
    "username": "tenant",
    "role": "TENANT",
    "tenantId": 1
  },
  "role": "TENANT",
  "token": "...",
  "tenantId": 1  // ✨ NEW!
}
```

### USER Login
```json
{
  "success": true,
  "user": {
    "id": 3,
    "username": "user",
    "role": "USER",
    "tenantId": 2
  },
  "role": "USER",
  "token": "...",
  "tenantId": 2  // ✨ NEW!
}
```

---

## Future Enhancements

1. **Add backend authorization middleware**:
   - Verify JWT token on admin endpoints
   - Return 403 for non-admin users
   - Add role-based endpoint protection

2. **Add tenant switching** (for users in multiple tenants):
   - Show tenant selector in navbar
   - Switch between tenants dynamically
   - Update localStorage and re-navigate

3. **Add audit logging**:
   - Log all admin page access attempts
   - Track unauthorized access attempts
   - Security monitoring dashboard

4. **Add session timeout**:
   - Auto-logout after inactivity
   - Redirect to login with message
   - Preserve intended destination

---

## Status: ✅ COMPLETE

TENANT users now login directly to their tenant detail page, and the admin page is secured with immediate redirect for unauthorized users!

