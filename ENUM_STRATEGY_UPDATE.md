# Updated Strategy Factory to Use ArchiveStandard Enum

## Changes Made

### 1. Updated ArchiveStandard Enum
**File:** `/src/main/java/com/dmc/archiving/archive/model/ArchiveStandard.java`

Added all 9 archiving standards to the enum:
```java
public enum ArchiveStandard {
    NOARK5,
    OAIS,
    PREMIS,
    DUBLIN_CORE,
    METS,
    EAD,
    BAGIT,
    ISADG,
    MODS
}
```

### 2. Updated ArchiveStrategyFactory
**File:** `/src/main/java/com/dmc/archiving/archive/strategy/ArchiveStrategyFactory.java`

**Key Changes:**
- Changed internal map from `Map<String, ArchiveStrategy>` to `Map<ArchiveStandard, ArchiveStrategy>`
- Added primary method: `getStrategy(ArchiveStandard standard)` - uses enum
- Kept backward compatibility: `getStrategy(String standardName)` - converts String to enum
- Updated `hasStrategy()` with both enum and String overloads
- Direct registration using enum keys

**Before:**
```java
private final Map<String, ArchiveStrategy> strategies;

public ArchiveStrategy getStrategy(String standardName) {
    String normalizedName = standardName.toUpperCase().replace(" ", "_");
    return strategies.getOrDefault(normalizedName, getDefaultStrategy(normalizedName));
}
```

**After:**
```java
private final Map<ArchiveStandard, ArchiveStrategy> strategies;

// Primary method - uses enum
public ArchiveStrategy getStrategy(ArchiveStandard standard) {
    if (standard == null) {
        return getDefaultStrategy("UNKNOWN");
    }
    return strategies.getOrDefault(standard, getDefaultStrategy(standard.name()));
}

// Backward compatibility - converts String to enum
public ArchiveStrategy getStrategy(String standardName) {
    if (standardName == null) {
        return getDefaultStrategy("UNKNOWN");
    }
    try {
        String normalizedName = standardName.toUpperCase().replace(" ", "_");
        ArchiveStandard standard = ArchiveStandard.valueOf(normalizedName);
        return getStrategy(standard);
    } catch (IllegalArgumentException e) {
        return getDefaultStrategy(standardName);
    }
}
```

### 3. Updated ArchiveController
**File:** `/src/main/java/com/dmc/archiving/archive/ArchiveController.java`

**Before:**
```java
"archive_" + archiveId + "_" + archive.getStandard() + "_export.json"
```

**After:**
```java
"archive_" + archiveId + "_" + archive.getStandard().name() + "_export.json"
```

Uses `.name()` to get the enum name as String.

## Benefits

### 1. **Type Safety**
- ✅ Compile-time checking of standard names
- ✅ No invalid standard values at runtime
- ✅ IDE autocomplete for standard names

### 2. **Better Performance**
- ✅ HashMap lookup using enum (faster than String)
- ✅ No string normalization needed for enum path
- ✅ Enum comparison is more efficient

### 3. **Cleaner Code**
- ✅ Explicit enum type instead of magic strings
- ✅ Clear contract in method signatures
- ✅ Less error-prone

### 4. **Backward Compatibility**
- ✅ String overload still available for REST endpoints
- ✅ Automatic conversion from String to enum
- ✅ Graceful fallback for invalid strings

## Usage Examples

### From Java Code (Enum)
```java
// Direct enum usage - PREFERRED
Archive archive = archiveService.getArchiveById(1L);
ArchiveStrategy strategy = strategyFactory.getStrategy(archive.getStandard());

// Or explicitly
ArchiveStrategy strategy = strategyFactory.getStrategy(ArchiveStandard.NOARK5);
```

### From REST Endpoint (String)
```java
// String parameter from path variable - still works
@GetMapping("/api/standards/{standardName}/requirements")
public ResponseEntity<?> getMetadataRequirements(@PathVariable String standardName) {
    ArchiveStrategy strategy = strategyFactory.getStrategy(standardName);
    // Automatically converts "NOARK5" string to ArchiveStandard.NOARK5 enum
}
```

### GraphQL Input
```graphql
mutation CreateArchive($input: CreateArchiveInput!) {
  createArchive(input: $input) {
    id
    standard  # Returns enum value: NOARK5, OAIS, etc.
  }
}
```

## Migration Guide

### Old Code
```java
ArchiveStrategy strategy = strategyFactory.getStrategy("NOARK5");
```

### New Code (Preferred)
```java
ArchiveStrategy strategy = strategyFactory.getStrategy(ArchiveStandard.NOARK5);
```

### Still Works (Backward Compatible)
```java
ArchiveStrategy strategy = strategyFactory.getStrategy("NOARK5"); // Still works!
```

## Validation

The factory now validates standard names more strictly:

**Valid Inputs:**
- `ArchiveStandard.NOARK5` ✅
- `"NOARK5"` ✅ (converted to enum)
- `"noark5"` ✅ (normalized and converted)
- `"dublin_core"` ✅ (normalized to DUBLIN_CORE)

**Invalid Inputs:**
- `"INVALID_STANDARD"` → Returns DefaultArchiveStrategy
- `null` → Returns DefaultArchiveStrategy("UNKNOWN")

## Testing

### Test with Enum
```java
@Test
void testGetStrategyWithEnum() {
    ArchiveStrategy strategy = factory.getStrategy(ArchiveStandard.NOARK5);
    assertEquals("NOARK5", strategy.getStandardName());
}
```

### Test with String
```java
@Test
void testGetStrategyWithString() {
    ArchiveStrategy strategy = factory.getStrategy("NOARK5");
    assertEquals("NOARK5", strategy.getStandardName());
}
```

### Test Invalid String
```java
@Test
void testGetStrategyWithInvalidString() {
    ArchiveStrategy strategy = factory.getStrategy("INVALID");
    assertNotNull(strategy); // Returns default strategy
}
```

## Summary

✅ **ArchiveStandard enum** - Updated with all 9 standards  
✅ **ArchiveStrategyFactory** - Uses enum internally, String for compatibility  
✅ **ArchiveController** - Uses enum directly from Archive model  
✅ **Type safety** - Compile-time checking  
✅ **Performance** - Faster enum-based lookups  
✅ **Backward compatible** - String methods still work  
✅ **No compilation errors** - All code compiles successfully  

The factory now uses proper type-safe enums while maintaining backward compatibility with String-based calls! 🎉
