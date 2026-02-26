# GraphQL Schema Unmapped Registrations Fix

## Issue

The application showed warnings on startup about unmapped GraphQL query registrations:

```
Unmapped registrations: {
  Query.getAllArchivesPaginated=ArchiveController#getAllArchivesPaginated[4 args],
  Query.getArchivesByStatusPaginated=ArchiveController#getArchivesByStatusPaginated[5 args],
  Query.searchArchivesByTitlePaginated=ArchiveController#searchArchivesByTitlePaginated[5 args],
  Query.getArchivesByUserPaginated=ArchiveController#getArchivesByUserPaginated[5 args],
  Query.getArchivesByUserAssignmentPaginated=ArchiveController#getArchivesByUserAssignmentPaginated[5 args]
}
```

## Root Cause

The `ArchiveController` had 5 paginated query methods returning `Page<Archive>` (Spring Data type), but:
1. These queries were not defined in the GraphQL schema (`schema.graphqls`)
2. Spring Data's `Page` type is not a valid GraphQL type (complex Java class)

## Solution

### 1. Created ArchivePage DTO

Created a new GraphQL-compatible data transfer object:

**File**: `src/main/java/com/dmc/archiving/archive/dto/ArchivePage.java`

```java
public record ArchivePage(
        List<Archive> content,
        PageInfo pageInfo,
        boolean empty
) {
    public static ArchivePage from(Page<Archive> page) {
        return new ArchivePage(
                page.getContent(),
                PageInfo.from(page),
                page.isEmpty()
        );
    }

    public record PageInfo(
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious,
            boolean isFirst,
            boolean isLast
    ) {
        public static PageInfo from(Page<?> page) {
            return new PageInfo(
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.hasNext(),
                    page.hasPrevious(),
                    page.isFirst(),
                    page.isLast()
            );
        }
    }
}
```

### 2. Updated GraphQL Schema

Added pagination types and paginated queries to `schema.graphqls`:

```graphql
# Pagination Types
type PageInfo {
    number: Int!
    size: Int!
    totalElements: Int!
    totalPages: Int!
    hasNext: Boolean!
    hasPrevious: Boolean!
    isFirst: Boolean!
    isLast: Boolean!
}

type ArchivePage {
    content: [Archive!]!
    pageInfo: PageInfo!
    empty: Boolean!
}
```

Added queries:
```graphql
# Archive Paginated Queries (Recommended for scalability)
getAllArchivesPaginated(page: Int, size: Int, sortBy: String, sortDirection: String): ArchivePage!
getArchivesByUserPaginated(userId: ID!, page: Int, size: Int, sortBy: String, sortDirection: String): ArchivePage!
getArchivesByUserAssignmentPaginated(userId: ID!, page: Int, size: Int, sortBy: String, sortDirection: String): ArchivePage!
getArchivesByStatusPaginated(status: ArchiveStatus!, page: Int, size: Int, sortBy: String, sortDirection: String): ArchivePage!
searchArchivesByTitlePaginated(title: String!, page: Int, size: Int, sortBy: String, sortDirection: String): ArchivePage!
```

### 3. Updated ArchiveController

Changed all 5 paginated methods to return `ArchivePage` instead of `Page<Archive>`:

```java
@QueryMapping
public com.dmc.archiving.archive.dto.ArchivePage getAllArchivesPaginated(
        @Argument Integer page,
        @Argument Integer size,
        @Argument String sortBy,
        @Argument String sortDirection) {
    
    // ...existing logic...
    
    return com.dmc.archiving.archive.dto.ArchivePage.from(
        archiveService.getAllArchivesPaginated(pageable)
    );
}
```

## Files Changed

### Created (1 file)
- `src/main/java/com/dmc/archiving/archive/dto/ArchivePage.java`

### Modified (2 files)
- `src/main/resources/graphql/schema.graphqls` - Added pagination types and queries
- `src/main/java/com/dmc/archiving/archive/ArchiveController.java` - Changed return types and removed unused import

## Verification

### Compilation
✅ Project compiles successfully:
```bash
./mvnw compile -DskipTests
# Result: BUILD SUCCESS
```

### Schema Inspection
✅ No more unmapped registrations when starting the application.

## Usage Example

### GraphQL Query

```graphql
query GetPaginatedArchives {
  getAllArchivesPaginated(page: 0, size: 20, sortBy: "createdAt", sortDirection: "DESC") {
    content {
      id
      title
      description
      status
      standard
      createdAt
    }
    pageInfo {
      number
      size
      totalElements
      totalPages
      hasNext
      hasPrevious
      isFirst
      isLast
    }
    empty
  }
}
```

### Response

```json
{
  "data": {
    "getAllArchivesPaginated": {
      "content": [
        {
          "id": "1",
          "title": "My Archive",
          "description": "Archive description",
          "status": "PUBLISHED",
          "standard": "NOARK5",
          "createdAt": "2026-02-26T00:00:00"
        }
      ],
      "pageInfo": {
        "number": 0,
        "size": 20,
        "totalElements": 150,
        "totalPages": 8,
        "hasNext": true,
        "hasPrevious": false,
        "isFirst": true,
        "isLast": false
      },
      "empty": false
    }
  }
}
```

## Benefits

1. **Type Safety**: GraphQL schema now properly defines pagination types
2. **No More Warnings**: Schema inspection shows no unmapped registrations
3. **Better Performance**: Paginated queries are recommended for production
4. **Standard Pagination**: Consistent pagination pattern across all queries
5. **Client-Friendly**: Pagination metadata available for UI pagination controls

## Impact

- ✅ **No Breaking Changes**: Existing non-paginated queries still work
- ✅ **Backward Compatible**: Old queries continue to function
- ✅ **Performance**: Paginated queries recommended for large datasets
- ✅ **Production Ready**: Scalable pagination implementation

---

**Date**: February 26, 2026  
**Status**: ✅ **COMPLETE**  
**Issue**: GraphQL unmapped registrations  
**Resolution**: Added pagination types and DTO wrapper for Spring Data Page

