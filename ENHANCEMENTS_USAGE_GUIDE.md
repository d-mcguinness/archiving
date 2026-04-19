# 🚀 Quick Start: Using the New Enhancements

This guide shows you how to use the newly implemented enhancements in the Archiving System.

---

## 🎯 1. Dynamic Search with Specifications

The new Specification pattern allows you to build complex queries dynamically.

### Basic Search

```java
// Search archives by keyword
ArchiveSearchCriteria criteria = ArchiveSearchCriteria.builder()
    .keyword("financial")
    .build();

Page<Archive> results = archiveService.searchArchives(criteria, pageable);
```

### Advanced Search

```java
// Combine multiple filters
ArchiveSearchCriteria criteria = ArchiveSearchCriteria.builder()
    .tenantId(1L)
    .status(ArchiveStatus.ACTIVE)
    .standard(ArchiveStandard.NOARK5)
    .keyword("report")
    .fromDate(LocalDateTime.now().minusMonths(6))
    .toDate(LocalDateTime.now())
    .build();

Pageable pageable = PageRequest.of(
    0,              // page number
    20,             // page size
    Sort.by("createdAt").descending()
);

Page<Archive> results = archiveService.searchArchives(criteria, pageable);
```

### GraphQL Example

```graphql
query SearchArchives {
  searchArchives(
    criteria: {
      tenantId: 1
      status: ACTIVE
      keyword: "financial"
      fromDate: "2025-08-01T00:00:00"
    }
    page: 0
    size: 20
  ) {
    content {
      id
      title
      status
      createdAt
    }
    totalElements
    totalPages
  }
}
```

---

## 🔄 2. Error Handling

All errors now return consistent responses.

### Handling Exceptions in Your Code

```java
// Just throw exceptions - they're handled automatically
@MutationMapping
public Archive createArchive(@Argument CreateArchiveInput input) {
    // Validation happens automatically with @Valid
    return archiveService.createArchive(input);
}

// Custom exception
throw new ResourceNotFoundException("Archive", archiveId);
// Returns: 404 with proper error message

// Validation errors
@Data
public class CreateArchiveInput {
    @NotBlank(message = "Title is required")
    private String title;
}
// Returns: 400 with field-specific errors
```

### Error Response Format

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Archive with ID 123 not found",
  "timestamp": "2026-02-26T10:30:00"
}
```

### Validation Error Response

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "timestamp": "2026-02-26T10:30:00",
  "validationErrors": {
    "title": "Title is required",
    "userId": "User ID must be positive"
  }
}
```

---

## 💾 3. Caching

Caching is automatic! Here's how it works:

### Cached Operations

```java
// First call - hits database
Archive archive = archiveService.getArchiveById(1L); // ~200ms

// Second call - from cache
Archive archive = archiveService.getArchiveById(1L); // ~10ms (95% faster!)
```

### Cache Regions

| Cache Name | Purpose | TTL |
|------------|---------|-----|
| `archives` | Individual archives | 10 min |
| `archivesByTenant` | Tenant-filtered lists | 10 min |
| `archiveWithRelations` | Archives with joins | 10 min |
| `archiveStatistics` | Dashboard stats | 30 min |

### Cache Eviction

Caches are automatically cleared on updates:

```java
// This automatically clears relevant caches
archiveService.updateArchive(id, input);
archiveService.deleteArchive(id);
```

### Manual Cache Control (if needed)

```java
@Autowired
private CacheManager cacheManager;

// Clear specific cache
cacheManager.getCache("archives").clear();

// Clear all caches
cacheManager.getCacheNames().forEach(name -> 
    cacheManager.getCache(name).clear()
);
```

---

## 📊 4. Database Indexes

Indexes are applied via SQL script.

### Apply Indexes

```bash
# Production (PostgreSQL)
psql -U archiving_user -d archiving -f add_performance_indexes.sql

# Development (if using PostgreSQL in Docker)
docker exec -i postgres_container psql -U archiving_user -d archiving < add_performance_indexes.sql
```

### Verify Indexes

```sql
-- Check what indexes exist
SELECT tablename, indexname, indexdef 
FROM pg_indexes 
WHERE schemaname = 'public' 
ORDER BY tablename, indexname;

-- Check index usage statistics
SELECT 
    schemaname, tablename, indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

---

## 🔐 5. Transaction Management

All service methods now use proper transactions.

### Read Operations

```java
@Transactional(readOnly = true)  // Optimized for reads
public Archive getArchiveById(Long id) {
    return archiveRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Archive", id));
}
```

### Write Operations

```java
@Transactional  // Full ACID guarantees
public Archive createArchive(CreateArchiveInput input) {
    // If any exception occurs, everything rolls back
    Archive archive = new Archive(...);
    archiveRepository.save(archive);
    sendNotification(archive);  // Also part of transaction
    return archive;
}
```

### Benefits

- ✅ Automatic rollback on errors
- ✅ Data integrity guaranteed
- ✅ Better performance for read-only operations
- ✅ Deadlock prevention

---

## 📝 6. Validation

Input validation is automatic with Bean Validation.

### Annotate Your DTOs

```java
@Data
public class CreateArchiveInput {
    
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;
    
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be 3-255 characters")
    private String title;
    
    @Email(message = "Must be a valid email")
    private String ownerEmail;
    
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Invalid format")
    private String code;
}
```

### Use @Valid in Controllers

```java
@MutationMapping
public Archive createArchive(@Valid @Argument CreateArchiveInput input) {
    // Validation happens automatically
    // If validation fails, returns 400 with error details
    return archiveService.createArchive(input);
}
```

---

## 📈 7. Performance Monitoring

### Check Cache Statistics

```java
// In your service or controller
@Autowired
private CacheManager cacheManager;

public Map<String, Object> getCacheStats() {
    Map<String, Object> stats = new HashMap<>();
    
    cacheManager.getCacheNames().forEach(cacheName -> {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                (com.github.benmanes.caffeine.cache.Cache<Object, Object>) 
                ((CaffeineCache) cache).getNativeCache();
            
            CacheStats cacheStats = nativeCache.stats();
            Map<String, Object> cacheInfo = new HashMap<>();
            cacheInfo.put("hitRate", cacheStats.hitRate());
            cacheInfo.put("hitCount", cacheStats.hitCount());
            cacheInfo.put("missCount", cacheStats.missCount());
            cacheInfo.put("evictionCount", cacheStats.evictionCount());
            
            stats.put(cacheName, cacheInfo);
        }
    });
    
    return stats;
}
```

### Query Performance

```sql
-- Check slow queries (PostgreSQL)
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    max_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Enable if not already active:
-- CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
```

---

## 🧪 8. Testing the Enhancements

### Test Error Handling

```bash
# Test 404 error
curl http://localhost:2020/api/archives/99999

# Expected response:
{
  "status": 404,
  "error": "Not Found",
  "message": "Archive with ID 99999 not found",
  "timestamp": "2026-02-26T..."
}
```

### Test Validation

```bash
# Test validation error
curl -X POST http://localhost:2020/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { createArchive(input: { title: \"\", userId: -1 }) { id } }"
  }'

# Expected: Validation errors for empty title and negative userId
```

### Test Caching

```bash
# First request (slow - hits database)
time curl http://localhost:2020/api/archives/1

# Second request (fast - from cache)
time curl http://localhost:2020/api/archives/1
```

### Test Search

```bash
curl -X POST http://localhost:2020/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { searchArchives(criteria: { keyword: \"test\", tenantId: 1 }, page: 0, size: 10) { content { id title } totalElements } }"
  }'
```

---

## 🔍 Common Use Cases

### 1. Search Archives for a Tenant

```java
ArchiveSearchCriteria criteria = ArchiveSearchCriteria.builder()
    .tenantId(tenantId)
    .build();

Page<Archive> archives = archiveService.searchArchives(
    criteria, 
    PageRequest.of(0, 20)
);
```

### 2. Find Active Archives Modified Recently

```java
ArchiveSearchCriteria criteria = ArchiveSearchCriteria.builder()
    .status(ArchiveStatus.ACTIVE)
    .updatedAfter(LocalDateTime.now().minusDays(7))
    .build();

Page<Archive> recent = archiveService.searchArchives(criteria, pageable);
```

### 3. Multi-Status Search

```java
ArchiveSearchCriteria criteria = ArchiveSearchCriteria.builder()
    .statuses(new ArchiveStatus[]{
        ArchiveStatus.ACTIVE, 
        ArchiveStatus.PUBLISHED
    })
    .build();

Page<Archive> archives = archiveService.searchArchives(criteria, pageable);
```

### 4. Full-Text Search

```java
ArchiveSearchCriteria criteria = ArchiveSearchCriteria.builder()
    .keyword("financial report Q1 2026")
    .build();

// Searches in both title and description
Page<Archive> results = archiveService.searchArchives(criteria, pageable);
```

---

## 📚 Additional Resources

- **Full Proposal**: `DESIGN_IMPROVEMENTS_PROPOSAL.md`
- **Quick Reference**: `DESIGN_IMPROVEMENTS_QUICK_REFERENCE.md`
- **Implementation Details**: `ENHANCEMENTS_IMPLEMENTED.md`
- **Design Patterns**: `DESIGN_PATTERNS_ANALYSIS.md`

---

## 🐛 Troubleshooting

### Cache Not Working

```bash
# Check if caching is enabled
# In application.properties
spring.cache.type=caffeine

# Verify CacheConfig bean is loaded
# Check logs for: "CacheManager initialized"
```

### Indexes Not Applied

```bash
# Check PostgreSQL connection
psql -U archiving_user -d archiving -c "\di"

# Manually create missing index
CREATE INDEX idx_archive_tenant_id ON archives(tenant_id);
```

### Validation Not Triggered

```java
// Make sure @Valid is present
@MutationMapping
public Archive create(@Valid @Argument CreateArchiveInput input) { ... }

// Check spring-boot-starter-validation is in pom.xml
```

---

## ✅ Checklist: Verify Enhancements Work

- [ ] Error responses are consistent (404, 400, 500)
- [ ] Caching reduces response time significantly
- [ ] Database queries use indexes (check EXPLAIN)
- [ ] Search works with multiple criteria
- [ ] Validation errors show field-specific messages
- [ ] Transactions rollback on errors
- [ ] Logs show all operations

---

**Questions?** Check the full documentation or raise an issue!

