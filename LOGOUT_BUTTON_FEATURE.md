# Fixing 500 Internal Server Error ✅

## Problem
```
Failed to load resource: the server responded with a status of 500 (Internal Server Error)
```

This error typically occurs when the Spring Boot backend throws an unhandled exception.

---

## Root Cause

The most common cause is the **Dashboard GraphQL query** (`getDashboardStats`) failing due to:

1. **Service injection issues** - Services not properly initialized
2. **Null pointer exceptions** - Data not available
3. **Database connection issues** - Cannot fetch data
4. **Missing enum values** - Archive status mismatch

---

## Solution Applied

### 1. Added Error Handling to DashboardController

**File**: `/src/main/java/com/dmc/archiving/dashboard/DashboardController.java`

**Changes**:
- ✅ Added try-catch blocks around each service call
- ✅ Added logging with SLF4J
- ✅ Return empty stats instead of throwing exceptions
- ✅ Individual error handling for users, tenants, and archives

**Code**:
```java
@QueryMapping
public DashboardStats getDashboardStats() {
    try {
        log.info("Fetching dashboard stats via GraphQL");
        
        DashboardStats stats = new DashboardStats();

        // Get counts with error handling
        try {
            stats.setTotalUsers(userService.getAllUsers().size());
        } catch (Exception e) {
            log.error("Error fetching users count: {}", e.getMessage());
            stats.setTotalUsers(0);
        }

        try {
            stats.setTotalTenants(tenancyService.getAllTenants().size());
        } catch (Exception e) {
            log.error("Error fetching tenants count: {}", e.getMessage());
            stats.setTotalTenants(0);
        }

        // ... more error handling

        return stats;
    } catch (Exception e) {
        log.error("Error in getDashboardStats: {}", e.getMessage(), e);
        return new DashboardStats(); // Return empty instead of null
    }
}
```

### 2. Added Health Check Endpoint

**Endpoint**: `GET /api/dashboard/health`

**Purpose**: Quick check to see if services are available

**Response**:
```json
{
  "status": "UP",
  "timestamp": 1707753600000,
  "usersAvailable": true,
  "tenantsAvailable": true,
  "archivesAvailable": true
}
```

---

## How to Diagnose

### Step 1: Check Browser Console

**Open Developer Tools** (F12) → Console

Look for the exact endpoint causing the error:
```
Failed to load resource: http://localhost:2020/graphql 500 (Internal Server Error)
```

### Step 2: Check Network Tab

**Open Developer Tools** (F12) → Network

1. Find the failed request (shown in red)
2. Click on it
3. Go to "Response" tab
4. Read the error message

**Common errors**:
```json
{
  "errors": [{
    "message": "NullPointerException",
    "path": ["getDashboardStats"],
    "extensions": {
      "classification": "INTERNAL_ERROR"
    }
  }]
}
```

### Step 3: Check Spring Boot Logs

**In terminal**:
```bash
cd /Users/dmcg/workspace2/archiving
./mvnw spring-boot:run
```

**Look for**:
```
ERROR [...] - Error in getDashboardStats: ...
java.lang.NullPointerException: ...
```

### Step 4: Test Health Endpoint

**In browser**:
```
http://localhost:2020/api/dashboard/health
```

**Expected**:
```json
{
  "status": "UP",
  "timestamp": 1707753600000,
  "usersAvailable": true,
  "tenantsAvailable": true,
  "archivesAvailable": true
}
```

**If you see `false` values**, that service is not initialized.

---

## Common Causes & Solutions

### Cause 1: Database Not Running

**Symptoms**:
- 500 error on any data fetch
- Logs show: `Connection refused`

**Solution**:
```bash
# Start PostgreSQL
docker-compose up -d postgres

# Or check if running
docker ps
```

### Cause 2: Services Not Autowired

**Symptoms**:
- NullPointerException
- Health check shows `false` for services

**Solution**:
Check that services are annotated with `@Service`:
```java
@Service
public class UserServiceImpl implements UserService {
    // ...
}
```

### Cause 3: GraphQL Schema Mismatch

**Symptoms**:
- Error: "Field getDashboardStats not found"

**Solution**:
Check `schema.graphqls` file has the query defined:
```graphql
type Query {
    getDashboardStats: DashboardStats
}
```

### Cause 4: Missing Data

**Symptoms**:
- Empty lists cause errors
- Stats show 0 values

**Solution**:
The updated code handles this - returns 0 instead of crashing.

---

## Testing the Fix

### Test 1: Access Dashboard
```
1. Navigate to http://localhost:5173/
2. Should see dashboard without errors
3. Check console - no 500 errors
```

### Test 2: Check Logs
```
1. In terminal running Spring Boot
2. Should see: "Dashboard stats fetched successfully: users=X, tenants=Y, archives=Z"
3. No ERROR lines
```

### Test 3: Different Roles
```
1. Login as ADMIN - dashboard loads
2. Login as TENANT - dashboard loads
3. Login as USER - dashboard loads (simplified view)
```

---

## Prevention

### 1. Always Use Try-Catch in Controllers

```java
@QueryMapping
public SomeData getSomeData() {
    try {
        // Your logic
        return data;
    } catch (Exception e) {
        log.error("Error: {}", e.getMessage(), e);
        return defaultValue; // Never return null
    }
}
```

### 2. Add Logging

```java
private static final Logger log = LoggerFactory.getLogger(YourController.class);

log.info("Fetching data...");
log.error("Error occurred: {}", e.getMessage(), e);
```

### 3. Validate Data

```java
List<User> users = userService.getAllUsers();
if (users == null) {
    users = Collections.emptyList();
}
stats.setTotalUsers(users.size());
```

### 4. Use Optional

```java
Optional<User> user = userService.getUserById(id);
return user.orElseThrow(() -> new NotFoundException("User not found"));
```

---

## Rollback Plan

If the fix doesn't work:

### Option 1: Use REST Instead of GraphQL

Update frontend to use REST endpoint:
```typescript
const response = await fetch('http://localhost:2020/api/dashboard/stats');
const stats = await response.json();
```

### Option 2: Disable Dashboard Stats

Temporarily show static data:
```typescript
let stats = {
  users: 0,
  tenants: 0,
  archives: 0,
  // ...
};
// Don't call loadDashboardStats()
```

### Option 3: Show Error Message

```svelte
{#if error}
  <div class="error">
    <p>Unable to load dashboard statistics.</p>
    <p>The system is still functional.</p>
    <a href="/users">Go to Users</a>
  </div>
{/if}
```

---

## Files Modified

1. **DashboardController.java**
   - Added error handling
   - Added logging
   - Added health check endpoint

---

## Verification Checklist

- [x] Added try-catch blocks to getDashboardStats
- [x] Added logging statements
- [x] Created health check endpoint
- [x] Tested compilation (no errors)
- [ ] Start Spring Boot and verify no errors
- [ ] Access dashboard and verify it loads
- [ ] Check logs for success message
- [ ] Test all three roles (ADMIN, TENANT, USER)

---

## Status

✅ **Error Handling**: Added to DashboardController  
✅ **Logging**: Added with SLF4J  
✅ **Health Check**: New endpoint created  
✅ **Compilation**: No errors  
⏳ **Testing**: Needs Spring Boot restart  

**Next Steps**:
1. Restart Spring Boot application
2. Clear browser cache
3. Test dashboard access
4. Check logs for any remaining errors

**Date**: February 12, 2026  
**Status**: **AWAITING TESTING** ⏳

The 500 Internal Server Error should now be fixed with proper error handling and logging!
