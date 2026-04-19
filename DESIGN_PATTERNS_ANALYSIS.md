# Design Patterns Analysis - Archiving System

## Overview

This document provides a comprehensive analysis of the design patterns identified in the Archiving System project, which is a full-stack application built with Spring Boot backend and SvelteKit frontend.

---

## 🏗️ Architectural Patterns

### 1. **Modular Monolith (Spring Modulith)**

**Location**: Backend architecture
**Implementation**: `src/main/java/com/dmc/archiving/`

The application uses **Spring Modulith** to create well-defined module boundaries within a monolithic application.

**Modules Identified**:
- `user` - User management
- `tenancy` - Multi-tenant functionality
- `archive` - Archive management
- `document` - Document handling
- `sip` - Submission Information Packages
- `aip` - Archival Information Packages
- `dip` - Dissemination Information Packages
- `pkg` - Package management
- `storage` - Cloud storage operations
- `auth` - Authentication/Authorization
- `dashboard` - Statistics and metrics

**Benefits**:
- Clear module boundaries
- Enforced encapsulation
- Easy to test and maintain
- Can be split into microservices later if needed

**Evidence**:
```java
// package-info.java defines module boundaries
package com.dmc.archiving.tenancy;
/**
 * Tenancy Module - Handles multi-tenant functionality.
 * External dependencies: user module (UserApi.userExists())
 * Exposed API: TenancyApi interface in the api package
 */
```

**Test Verification**:
```java
// ModulithStructureTest.java
@Test
void verifiesModularStructure() {
    modules.verify(); // Validates module boundaries
}
```

---

### 2. **Event-Driven Architecture**

**Location**: Backend inter-module communication
**Pattern**: Observer/Publisher-Subscriber

Modules communicate through **application events** to avoid direct dependencies and circular references.

**Implementation**:
- Events published when users are deleted
- Listeners in other modules respond to events
- Achieves loose coupling between modules

**Benefits**:
- Decoupled modules
- No circular dependencies
- Easy to add new listeners
- Asynchronous processing possible

**Example Flow**:
```
User Module                Tenancy Module
    |                           |
    | fires UserDeletedEvent    |
    |-------------------------->|
    |                           |
    |                    UserEventListener
    |                    handles cleanup
```

**Documentation**: See `CIRCULAR_DEPENDENCY_FIX.md`

---

## 🎨 Creational Patterns

### 3. **Factory Pattern**

**Location**: Multiple factories in backend
**Pattern**: Factory Method / Simple Factory

**Implementations**:

#### a) **SipGeneratorFactory** 
```java
@Component
public class SipGeneratorFactory {
    private final Map<ArchiveStandard, SipGenerator> generators;
    
    public SipGenerator getGenerator(ArchiveStandard standard) {
        return generators.getOrDefault(standard, defaultGenerator);
    }
}
```

Creates standard-specific SIP generators for:
- NOARK5, OAIS, PREMIS, Dublin Core, METS, EAD, BagIt, ISAD(G), MODS, E-ARK

#### b) **AipGeneratorFactory**
```java
@Component
public class AipGeneratorFactory {
    private final Map<ArchiveStandard, AipGenerator> generators;
    
    public AipGenerator getGenerator(ArchiveStandard standard) {
        return generators.getOrDefault(standard, defaultGenerator);
    }
}
```

Creates standard-specific AIP generators.

#### c) **ArchiveStrategyFactory** (Documented in STRATEGY_PATTERN_IMPLEMENTATION.md)
```java
@Component
public class ArchiveStrategyFactory {
    // Maps standard names to strategies
    public ArchiveStrategy getStrategy(String standardName) {
        return strategies.get(standardName);
    }
}
```

**Benefits**:
- Centralized object creation
- Easy to add new standards
- Runtime selection based on configuration
- Single Responsibility Principle

---

## 🔧 Behavioral Patterns

### 4. **Strategy Pattern** ⭐

**Location**: Archive operations
**Pattern**: Strategy Pattern with Factory

**Most Prominent Pattern in the System!**

**Structure**:
```
ArchiveStrategy (Interface)
    ↑
    |
AbstractArchiveStrategy (Base Implementation)
    ↑
    |
    ├── Noark5Strategy
    ├── OaisStrategy
    ├── PremisStrategy
    └── DefaultArchiveStrategy (for other standards)
```

**Strategy Interface**:
```java
public interface ArchiveStrategy {
    ValidationResult validate(Archive archive);
    Map<String, Object> export(Archive archive);
    Archive importArchive(Map<String, Object> data);
    Map<String, String> getMetadataRequirements();
    Map<String, Object> transformToStandard(Archive archive);
    String getStandardName();
}
```

**Implementations**:
- `Noark5Strategy` - Norwegian archival standard
- `OaisStrategy` - ISO 14721 standard (OAIS)
- `PremisStrategy` - Preservation metadata
- `DefaultArchiveStrategy` - Generic for Dublin Core, METS, EAD, BagIt, ISAD(G), MODS

**Benefits**:
- Different algorithms for different archiving standards
- Easy to add new standards (just create new strategy class)
- Standard-specific validation and export
- Follows Open/Closed Principle

**Usage Example**:
```java
ArchiveStrategy strategy = strategyFactory.getStrategy("NOARK5");
ValidationResult result = strategy.validate(archive);
Map<String, Object> exportData = strategy.export(archive);
```

**Documentation**: See `STRATEGY_PATTERN_IMPLEMENTATION.md`

---

### 5. **Template Method Pattern**

**Location**: Abstract base classes for generators and strategies
**Pattern**: Template Method

**Implementation in AbstractSipGenerator**:
```java
public abstract class AbstractSipGenerator implements SipGenerator {
    
    // Template method - defines algorithm skeleton
    public final SipSnapshot generate(Sip sip) {
        validate(sip);
        Map<String, Object> metadata = generateMetadata(sip);
        String content = formatContent(metadata);
        return createSnapshot(content);
    }
    
    // Steps to be implemented by subclasses
    protected abstract void validate(Sip sip);
    protected abstract Map<String, Object> generateMetadata(Sip sip);
    protected abstract String formatContent(Map<String, Object> metadata);
}
```

**Similar pattern in AbstractArchiveStrategy**:
```java
public abstract class AbstractArchiveStrategy implements ArchiveStrategy {
    
    // Template method
    public ValidationResult validate(Archive archive) {
        ValidationResult result = new ValidationResult();
        validateCommon(archive, result);
        validateStandard(archive, result); // Abstract - subclass implements
        return result;
    }
    
    protected abstract void validateStandard(Archive archive, ValidationResult result);
}
```

**Benefits**:
- Common logic in base class
- Customizable steps in subclasses
- Code reuse
- Consistent algorithm structure

---

### 6. **Repository Pattern**

**Location**: Data access layer
**Pattern**: Repository Pattern (Spring Data JPA)

**Repositories**:
- `UserRepository extends JpaRepository<User, Long>`
- `TenantRepository extends JpaRepository<Tenant, Long>`
- `ArchiveRepository extends JpaRepository<Archive, Long>`
- `DocumentRepository extends JpaRepository<Document, Long>`
- `SipRepository extends JpaRepository<Sip, Long>`
- `AipRepository extends JpaRepository<Aip, Long>`
- `DipRepository extends JpaRepository<Dip, Long>`
- `PackageRepository extends JpaRepository<Package, Long>`

**Benefits**:
- Abstraction over data source
- Separation of concerns
- Easy to mock for testing
- Query method generation

**Example**:
```java
public interface ArchiveRepository extends JpaRepository<Archive, Long> {
    List<Archive> findByStandard(ArchiveStandard standard);
    List<Archive> findByOwnerId(Long ownerId);
}
```

---

## 🔌 Structural Patterns

### 7. **Facade Pattern**

**Location**: Service layer
**Pattern**: Facade

Service classes act as facades to simplify complex subsystem interactions:

**Example - ArchiveService**:
```java
@Service
public class ArchiveService {
    @Autowired private ArchiveRepository repository;
    @Autowired private ArchiveStrategyFactory strategyFactory;
    @Autowired private ElementService elementService;
    
    public Archive createArchive(CreateArchiveInput input) {
        // Coordinates multiple operations
        Archive archive = new Archive();
        // ... populate archive
        validate(archive);
        save(archive);
        createDefaultElements(archive);
        return archive;
    }
}
```

**Benefits**:
- Simplified interface to complex subsystems
- Reduces coupling between client and subsystems
- Easier to use and understand

---

### 8. **Adapter Pattern**

**Location**: Storage layer
**Pattern**: Adapter (Interface Adaptation)

**CloudStorageService Interface**:
```java
public interface CloudStorageService {
    UploadResult uploadFile(MultipartFile file, Long userId);
    InputStream downloadFile(String fileKey);
    void deleteFile(String fileKey);
    boolean fileExists(String fileKey);
    String getPresignedUrl(String fileKey, int expirationMinutes);
}
```

**S3StorageService Implementation**:
```java
@Service
public class S3StorageService implements CloudStorageService {
    // Adapts AWS S3 API to our interface
    @Override
    public UploadResult uploadFile(MultipartFile file, Long userId) {
        // Translates our method calls to AWS S3 SDK calls
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(generateKey(file, userId))
            .build();
        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
    }
}
```

**Benefits**:
- Can swap AWS S3 for Google Cloud Storage or Azure Blob
- Consistent interface regardless of provider
- Adapts third-party APIs to our application's needs

---

### 9. **Dependency Injection (IoC Container)**

**Location**: Throughout backend
**Pattern**: Dependency Injection via Spring Framework

**Implementation**:
```java
@Service
public class ArchiveService {
    private final ArchiveRepository repository;
    private final ArchiveStrategyFactory strategyFactory;
    
    @Autowired // Constructor injection (preferred)
    public ArchiveService(
        ArchiveRepository repository,
        ArchiveStrategyFactory strategyFactory) {
        this.repository = repository;
        this.strategyFactory = strategyFactory;
    }
}
```

**Benefits**:
- Loose coupling
- Easy testing (mock dependencies)
- Configuration flexibility
- Lifecycle management

---

## 🎯 Frontend Patterns (SvelteKit)

### 10. **Model-View-ViewModel (MVVM)**

**Location**: Frontend architecture
**Pattern**: MVVM variant with Svelte stores

**Structure**:
- **Model**: GraphQL API responses
- **View**: Svelte components (.svelte files)
- **ViewModel**: Svelte stores + Apollo Cache

**Example - authStore.ts**:
```typescript
// ViewModel (Store)
export const auth = {
    isLoggedIn: writable(false),
    user: writable(null),
    role: writable(null),
    
    login(credentials) {
        // Business logic
    },
    logout() {
        // Clear state
    }
}
```

**View Usage**:
```svelte
<script>
    import { auth } from '$lib/stores/authStore';
</script>

{#if $auth.isLoggedIn}
    <span>Welcome {$auth.user?.name}</span>
{/if}
```

---

### 11. **Observer Pattern (Reactive Programming)**

**Location**: Frontend state management
**Pattern**: Observer Pattern via Svelte stores

**Implementation**:
```typescript
// Store (Subject)
const toastStore = writable([]);

// Component (Observer)
$: toasts = $toastStore; // Reactive subscription
```

Svelte's `$` prefix automatically subscribes to stores and updates when they change.

---

### 12. **Component Pattern**

**Location**: Frontend UI
**Pattern**: Reusable Component Architecture

**Reusable Components**:
- `Toast.svelte` - Notification system
- `UserCard.svelte` - User display
- `TenantCard.svelte` - Tenant display
- `ArchiveCard.svelte` - Archive display

**Composition Example**:
```svelte
<!-- Parent Component -->
<script>
    import UserCard from '$lib/components/UserCard.svelte';
</script>

{#each users as user}
    <UserCard {user} on:delete={handleDelete} />
{/each}
```

---

### 13. **Singleton Pattern**

**Location**: Frontend GraphQL client
**Pattern**: Singleton (Apollo Client instance)

**Implementation in apollo.ts**:
```typescript
let apolloClient;

export function getClient() {
    if (!apolloClient) {
        apolloClient = new ApolloClient({
            uri: GRAPHQL_URI,
            cache: new InMemoryCache()
        });
    }
    return apolloClient;
}
```

Single Apollo Client instance shared across the application.

---

## 🔄 Additional Patterns

### 14. **Builder Pattern (Implicit)**

**Location**: Entity creation
**Pattern**: Builder-like methods

GraphQL Input objects and entity construction follow builder-like patterns:

```java
// CreateArchiveInput acts like a builder
CreateArchiveInput.builder()
    .title("Archive")
    .standard(NOARK5)
    .content("{}")
    .build();
```

---

### 15. **Chain of Responsibility (Implicit)**

**Location**: GraphQL interceptors
**Pattern**: Chain of Responsibility

**GraphQlAuthInterceptor**:
```java
// Intercepts GraphQL requests
public class GraphQlAuthInterceptor implements WebGraphQlInterceptor {
    public Mono<WebGraphQlResponse> intercept(
        WebGraphQlRequest request, Chain chain) {
        // Process request
        return chain.next(request); // Pass to next handler
    }
}
```

---

### 16. **Role-Based Access Control (RBAC) Pattern**

**Location**: Authorization logic
**Pattern**: RBAC

**Implementation in +layout.svelte**:
```svelte
{#if $auth.role === 'ADMIN'}
    <!-- Admin-specific navigation -->
    <a href="/admin/tenants">Tenants</a>
    <a href="/admin/users">Users</a>
{:else if $auth.role === 'TENANT'}
    <!-- Tenant-specific navigation -->
    <a href="/tenants/{$auth.tenantId}/archives">Archives</a>
{:else if $auth.role === 'USER'}
    <!-- User-specific navigation -->
    <a href="/tenants/{$auth.tenantId}/users/{$auth.user.id}/documents">Documents</a>
{/if}
```

**Three Roles**:
- **ADMIN** - Full system access
- **TENANT** - Tenant-level access
- **USER** - User-level access (documents only)

---

## 📊 Pattern Summary

| Pattern | Category | Location | Purpose |
|---------|----------|----------|---------|
| **Modular Monolith** | Architectural | Backend | Module boundaries |
| **Event-Driven** | Architectural | Backend | Module communication |
| **Factory** | Creational | Backend | Object creation |
| **Strategy** ⭐ | Behavioral | Backend | Archiving standards |
| **Template Method** | Behavioral | Backend | Algorithm skeleton |
| **Repository** | Structural | Backend | Data access |
| **Facade** | Structural | Backend | Simplified interface |
| **Adapter** | Structural | Backend | Storage abstraction |
| **Dependency Injection** | Structural | Backend | IoC |
| **MVVM** | Architectural | Frontend | UI architecture |
| **Observer** | Behavioral | Frontend | Reactive state |
| **Component** | Structural | Frontend | UI composition |
| **Singleton** | Creational | Frontend | GraphQL client |
| **RBAC** | Security | Full-stack | Authorization |

---

## 🎯 Most Prominent Patterns

### Top 3 Design Patterns:

1. **Strategy Pattern** ⭐⭐⭐
   - Most extensively documented
   - Central to archive functionality
   - 9 different implementations for archiving standards
   - Complete with factory and documentation

2. **Modular Monolith (Spring Modulith)** ⭐⭐⭐
   - Architectural foundation
   - Clear module boundaries
   - Event-driven communication
   - Test-verified structure

3. **Repository Pattern** ⭐⭐
   - All data access
   - 8+ repositories
   - Clean separation of concerns

---

## 🔍 Pattern Relationships

```
┌─────────────────────────────────────────────────────┐
│                 Application Layer                    │
├─────────────────────────────────────────────────────┤
│  Controllers (Facade Pattern)                        │
│      ↓                                               │
│  Services (Business Logic)                           │
│      ↓                                               │
│  Strategy Pattern ←→ Factory Pattern                 │
│      ↓                                               │
│  Repository Pattern (Data Access)                    │
│      ↓                                               │
│  Adapter Pattern (Storage)                           │
└─────────────────────────────────────────────────────┘
        ↕ Events (Observer Pattern)
┌─────────────────────────────────────────────────────┐
│             Module Communication                     │
│  User Module ← Events → Tenancy Module               │
└─────────────────────────────────────────────────────┘
```

---

## 💡 Design Principles Applied

1. **SOLID Principles**:
   - ✅ **Single Responsibility**: Each module has one reason to change
   - ✅ **Open/Closed**: Strategy pattern allows extension without modification
   - ✅ **Liskov Substitution**: All strategies interchangeable
   - ✅ **Interface Segregation**: Focused interfaces (CloudStorageService)
   - ✅ **Dependency Inversion**: Depend on abstractions (ArchiveStrategy)

2. **DRY (Don't Repeat Yourself)**:
   - Template Method pattern for common logic
   - Abstract base classes

3. **Separation of Concerns**:
   - Modular architecture
   - Repository pattern
   - Service layer

4. **Loose Coupling**:
   - Event-driven communication
   - Dependency injection
   - Interface-based design

---

## 📚 Documentation References

- [Strategy Pattern Implementation](./STRATEGY_PATTERN_IMPLEMENTATION.md)
- [Circular Dependency Fix](./CIRCULAR_DEPENDENCY_FIX.md)
- [Backend Instructions](./INSTRUCTIONS.md)
- [README](./README.md)

---

## 🎓 Learning Resources

To understand these patterns better:

1. **Gang of Four (GoF) Design Patterns**
2. **Spring Framework Patterns**
3. **Domain-Driven Design (DDD)**
4. **Reactive Programming Patterns**

---

**Analysis Date**: February 26, 2026  
**Project Version**: 0.0.1-SNAPSHOT  
**Analyzer**: AI Code Analysis

