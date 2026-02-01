# Fixed: Circular Dependency Issue

## Problem

Application failed to start with circular dependency error:

```
UserServiceImpl → TenancyApi → GraphqlTenancyController → TenancyServiceImpl → UserApi → UserServiceImpl
```

The cycle was:
1. `UserServiceImpl` needed `TenancyApi` to remove users from tenants
2. `TenancyServiceImpl` needed `UserApi` to validate users
3. This created a circular dependency

## Root Cause

When we added the fix for the foreign key constraint violation, we injected `TenancyApi` into `UserServiceImpl`. However, `TenancyServiceImpl` already depends on `UserApi`, creating a circular dependency that Spring cannot resolve.

## Solution: Spring Application Events

Instead of direct dependency injection, we use **Spring's Event-Driven Architecture** to break the circular dependency.

### Pattern: Publish-Subscribe

```
UserServiceImpl → publishes → UserDeletedEvent → listened by → UserEventListener (Tenancy Module)
```

This breaks the dependency cycle because:
- User module doesn't depend on Tenancy module
- Tenancy module listens to events from User module
- No direct dependency between the modules

## Files Created/Modified

### 1. Created UserDeletedEvent (User API)

**File:** `/src/main/java/com/dmc/archiving/user/api/UserDeletedEvent.java`

```java
package com.dmc.archiving.user.api;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when a user is deleted.
 * Other modules can listen to this event to perform cleanup operations.
 * Part of the public API so other modules can listen to it.
 */
public class UserDeletedEvent extends ApplicationEvent {
    private final Long userId;

    public UserDeletedEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
```

**Why in `user.api` package?**
- Spring Modulith exposes only the `api` package to other modules
- Events must be in the API package so other modules can listen to them
- This allows Tenancy module to access the event without violating module boundaries

### 2. Updated UserServiceImpl

**File:** `/src/main/java/com/dmc/archiving/user/service/UserServiceImpl.java`

**Changes:**
- Removed `TenancyApi` dependency (this was causing the cycle)
- Injected `ApplicationEventPublisher` instead
- Publish `UserDeletedEvent` before deleting user

```java
@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;  // ← No more TenancyApi!

    @Override
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        
        // Publish event - no direct dependency on Tenancy module
        eventPublisher.publishEvent(new UserDeletedEvent(this, id));
        
        userRepository.deleteById(id);
        return true;
    }
}
```

### 3. Created UserEventListener (Tenancy Module)

**File:** `/src/main/java/com/dmc/archiving/tenancy/events/UserEventListener.java`

```java
package com.dmc.archiving.tenancy.events;

import com.dmc.archiving.tenancy.service.TenancyService;
import com.dmc.archiving.user.api.UserDeletedEvent;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class UserEventListener {

    private final TenancyService tenancyService;

    /**
     * When a user is deleted, remove them from all tenants first.
     * Order(0) ensures this runs before the actual deletion.
     */
    @EventListener
    @Order(0)
    @Transactional
    public void handleUserDeleted(UserDeletedEvent event) {
        // Remove user from all tenants to prevent FK constraint violation
        tenancyService.removeUserFromTenant(event.getUserId());
    }
}
```

**Key Points:**
- `@EventListener` annotation makes this method listen to `UserDeletedEvent`
- `@Order(0)` ensures it runs early (before the deletion completes)
- `@Transactional` ensures database operations are in a transaction
- Tenancy module can now react to user deletion without being called directly

### 4. Removed from TenancyApi

**File:** `/src/main/java/com/dmc/archiving/tenancy/api/TenancyApi.java`

Removed `removeUserFromAllTenants()` method - no longer needed since we use events.

## Architecture

### Before (Circular Dependency)
```
┌─────────────────────┐
│   UserServiceImpl   │
│  depends on         │
│  TenancyApi         │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│ GraphqlTenancy      │
│ Controller          │
│ implements          │
│ TenancyApi          │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│ TenancyServiceImpl  │
│ depends on          │
│ UserApi             │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│ UserServiceImpl     │ ← CYCLE!
│ implements UserApi  │
└─────────────────────┘
```

### After (Event-Driven - No Cycle)
```
┌─────────────────────┐
│   UserServiceImpl   │
│  publishes          │
│  UserDeletedEvent   │
└──────────┬──────────┘
           │
           │ (event)
           ↓
┌─────────────────────┐
│ UserEventListener   │
│ (Tenancy Module)    │
│  listens to event   │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│ TenancyServiceImpl  │
│ removes user from   │
│ tenants             │
└─────────────────────┘
```

## Benefits of Event-Driven Approach

### 1. **No Circular Dependencies**
- User module doesn't know about Tenancy module
- Tenancy module listens to User events
- One-way dependency: Tenancy → User API (events only)

### 2. **Loose Coupling**
- Modules communicate through events
- Easy to add new modules that react to user deletion
- No need to modify UserService when adding new cleanup logic

### 3. **Spring Modulith Compatible**
- Events in `user.api` package are exposed to other modules
- Follows Spring Modulith best practices
- Maintains module boundaries

### 4. **Extensible**
- Other modules (e.g., Archive) can listen to the same event
- Multiple listeners can react to one event
- Easy to add new cleanup operations

### 5. **Transactional**
- Event listeners run in transactions
- If cleanup fails, user deletion can be rolled back
- Maintains data integrity

## How It Works at Runtime

1. **User deletion requested**
   ```java
   userService.deleteUser(1L);
   ```

2. **Event published**
   ```java
   eventPublisher.publishEvent(new UserDeletedEvent(this, 1L));
   ```

3. **Event listener triggered** (in Tenancy module)
   ```java
   @EventListener
   public void handleUserDeleted(UserDeletedEvent event) {
       tenancyService.removeUserFromTenant(event.getUserId());
   }
   ```

4. **User removed from all tenants**
   - Removes rows from `user_tenant` table
   - Prevents foreign key constraint violation

5. **User deleted**
   ```java
   userRepository.deleteById(id);
   ```

6. **Success!** ✅

## Testing

### Test User Deletion After Fix

1. Start the application:
   ```bash
   mvn spring-boot:run
   ```

2. Create a user:
   ```graphql
   mutation {
     createUser(input: { name: "Test User", email: "test@test.com" }) {
       id
     }
   }
   ```

3. Add user to tenant:
   ```graphql
   mutation {
     addUserToTenant(userId: 1, tenantId: 1)
   }
   ```

4. Delete the user:
   ```graphql
   mutation {
     deleteUser(id: 1)
   }
   ```

5. ✅ **Success!** No circular dependency error, no FK constraint violation

## Alternative Solutions Considered

### Option 1: @Lazy Injection
```java
@Lazy
private final TenancyApi tenancyApi;
```
❌ **Rejected**: Breaks Spring Modulith validation, harder to debug

### Option 2: Setter Injection
```java
@Autowired
public void setTenancyApi(TenancyApi api) { ... }
```
❌ **Rejected**: Still creates circular dependency, just defers it

### Option 3: ApplicationContext Lookup
```java
context.getBean(TenancyApi.class)
```
❌ **Rejected**: Anti-pattern, hides dependencies

### Option 4: Events (✅ Chosen)
```java
eventPublisher.publishEvent(new UserDeletedEvent(this, id));
```
✅ **Selected**: Clean, maintainable, follows Spring Modulith patterns

## Related Patterns

### Future Event Listeners

Other modules can listen to the same event:

```java
// In Archive module
@Component
public class ArchiveUserEventListener {
    @EventListener
    public void handleUserDeleted(UserDeletedEvent event) {
        // Remove user assignments from archives
        archiveService.removeUserAssignments(event.getUserId());
    }
}
```

## Files Modified

1. ✅ Created `/user/api/UserDeletedEvent.java` - Event class
2. ✅ Updated `/user/service/UserServiceImpl.java` - Publish event instead of calling TenancyApi
3. ✅ Created `/tenancy/events/UserEventListener.java` - Listen to event and clean up
4. ✅ Updated `/tenancy/api/TenancyApi.java` - Removed removeUserFromAllTenants method
5. ✅ Updated `/tenancy/GraphqlTenancyController.java` - Removed implementation

## Summary

✅ **Problem**: Circular dependency between User and Tenancy modules  
✅ **Cause**: Direct dependency injection created a cycle  
✅ **Solution**: Event-driven architecture using Spring Application Events  
✅ **Pattern**: Publish-Subscribe  
✅ **Result**: No circular dependency, clean module boundaries  
✅ **Bonus**: Extensible architecture for future cleanup operations  

The application should now start successfully! 🎉
