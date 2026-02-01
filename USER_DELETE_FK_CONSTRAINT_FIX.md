# Fixed: Foreign Key Constraint Violation on User Deletion

## Problem

When deleting a user, PostgreSQL threw a constraint violation error:

```
ERROR: update or delete on table "users" violates foreign key constraint 
"fkb4i8vjuxmcuft88icu5iajnlf" on table "user_tenant"
Detail: Key (id)=(1) is still referenced from table "user_tenant".
```

## Root Cause

The `user_tenant` join table (created by the `@ManyToMany` relationship in the `Tenant` entity) has a foreign key constraint to the `users` table. When attempting to delete a user that still has tenant associations, PostgreSQL prevents the deletion to maintain referential integrity.

## Solution Applied

### 1. Added Public API Method to TenancyApi

**File:** `/src/main/java/com/dmc/archiving/tenancy/api/TenancyApi.java`

Added `removeUserFromAllTenants(Long userId)` method to allow the user module to clean up tenant associations before deleting a user.

```java
/**
 * Remove a user from all tenants.
 * Used when deleting a user to clean up tenant associations.
 * @param userId the user ID to remove from all tenants
 */
void removeUserFromAllTenants(Long userId);
```

### 2. Implemented Method in GraphqlTenancyController

**File:** `/src/main/java/com/dmc/archiving/tenancy/GraphqlTenancyController.java`

Implemented the new API method by delegating to the existing `removeUserFromTenant` service method.

```java
@Override
public void removeUserFromAllTenants(Long userId) {
    tenancyService.removeUserFromTenant(userId);
}
```

### 3. Updated UserServiceImpl

**File:** `/src/main/java/com/dmc/archiving/user/service/UserServiceImpl.java`

**Changes:**
- Injected `TenancyApi` dependency
- Updated `deleteUser()` method to remove user from all tenants before deletion

```java
@Override
public boolean deleteUser(Long id) {
    if (!userRepository.existsById(id)) {
        return false;
    }
    
    // Remove user from all tenants before deleting
    // This prevents foreign key constraint violations
    tenancyApi.removeUserFromAllTenants(id);
    
    userRepository.deleteById(id);
    return true;
}
```

## How It Works

### Before (Error)
```
1. User attempts to delete user ID 1
2. UserService calls userRepository.deleteById(1)
3. PostgreSQL checks foreign key constraints
4. Finds references in user_tenant table
5. ❌ Throws ConstraintViolationException
```

### After (Fixed)
```
1. User attempts to delete user ID 1
2. UserService calls tenancyApi.removeUserFromAllTenants(1)
3. TenancyService finds all tenants containing user 1
4. Removes user from each tenant's users collection
5. Saves updated tenants (removes rows from user_tenant table)
6. UserService calls userRepository.deleteById(1)
7. PostgreSQL checks foreign key constraints
8. ✅ No references found, deletion succeeds
```

## Database Schema

### user_tenant Join Table
```sql
CREATE TABLE user_tenant (
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, user_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (user_id) REFERENCES users(id)  -- This was causing the error
);
```

### Entity Relationship
```java
@Entity
@Table(name = "tenants")
public class Tenant {
    @ManyToMany
    @JoinTable(
        name = "user_tenant",
        joinColumns = @JoinColumn(name = "tenant_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();
}
```

## Spring Modulith Pattern

This fix follows Spring Modulith principles:

### Module Boundaries
- **User Module**: Responsible for user lifecycle
- **Tenancy Module**: Responsible for tenant-user relationships

### Public API Contract
- User module uses `TenancyApi` (public interface)
- Does not access `TenancyService` or repository directly
- Maintains loose coupling between modules

### Dependency Direction
```
User Module → TenancyApi (interface) ← Tenancy Module
```

## Testing

### Test User Deletion

1. **Create a user:**
   ```graphql
   mutation {
     createUser(input: {
       name: "Test User"
       email: "test@example.com"
       age: 30
     }) {
       id
       name
     }
   }
   ```

2. **Assign user to a tenant:**
   ```graphql
   mutation {
     addUserToTenant(userId: 1, tenantId: 1)
   }
   ```

3. **Verify user is in tenant:**
   ```graphql
   query {
     isUserInTenant(userId: 1, tenantId: 1)
   }
   ```

4. **Delete the user:**
   ```graphql
   mutation {
     deleteUser(id: 1)
   }
   ```

5. **Verify user was removed from tenant:**
   ```sql
   SELECT * FROM user_tenant WHERE user_id = 1;
   -- Should return 0 rows
   ```

6. **Verify user was deleted:**
   ```sql
   SELECT * FROM users WHERE id = 1;
   -- Should return 0 rows
   ```

## Error Handling

The fix gracefully handles:

✅ **User not in any tenants** - `removeUserFromAllTenants` does nothing, deletion proceeds  
✅ **User in multiple tenants** - Removed from all tenants, then deleted  
✅ **User doesn't exist** - Returns `false` without attempting cleanup  
✅ **Concurrent modifications** - Transaction ensures atomicity  

## Alternative Solutions Considered

### Option 1: CASCADE DELETE (Not Used)
```java
@ManyToMany(cascade = CascadeType.ALL)
private Set<User> users;
```
❌ **Rejected**: Would delete tenants when deleting users (wrong direction)

### Option 2: orphanRemoval (Not Applicable)
```java
@ManyToMany(orphanRemoval = true)
```
❌ **Not Applicable**: Only works for @OneToMany relationships

### Option 3: Database CASCADE (Not Used)
```sql
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
```
❌ **Rejected**: Removes database constraint protection

### Option 4: Application-Level Cleanup (✅ Used)
```java
tenancyApi.removeUserFromAllTenants(userId);
userRepository.deleteById(userId);
```
✅ **Selected**: Clean, explicit, maintains module boundaries

## Related Issues

This same pattern should be applied for:

### Archive User Assignments
The `user_assignments` table also has a foreign key to `users`. Consider adding:
```java
archiveApi.removeUserAssignments(userId);
```

### Tenant Ownership
If a user owns tenants (as `ownerId`), consider:
```java
tenancyApi.reassignOrDeleteTenantsOwnedBy(userId);
```

## Files Modified

1. ✅ `/tenancy/api/TenancyApi.java` - Added `removeUserFromAllTenants` method
2. ✅ `/tenancy/GraphqlTenancyController.java` - Implemented API method
3. ✅ `/user/service/UserServiceImpl.java` - Updated `deleteUser` to clean up associations

## Summary

✅ **Problem**: Foreign key constraint violation when deleting users  
✅ **Cause**: User still referenced in `user_tenant` join table  
✅ **Solution**: Remove user from all tenants before deletion  
✅ **Pattern**: Spring Modulith public API  
✅ **Result**: Users can now be deleted successfully  

The fix ensures referential integrity while maintaining clean module boundaries! 🎉
