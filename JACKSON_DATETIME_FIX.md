# Fixed: Jackson LocalDateTime Serialization Error

## Problem

When extracting an archive, the application threw a Jackson serialization error:

```
com.fasterxml.jackson.databind.exc.InvalidDefinitionException: 
Java 8 date/time type `java.time.LocalDateTime` not supported by default: 
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
(through reference chain: java.util.HashMap["createdAt"])
```

## Root Cause

The error occurred in the `extractArchive` endpoint when trying to serialize archive data to JSON. The archive export includes `LocalDateTime` fields (like `createdAt` and `updatedAt`), but Jackson's default ObjectMapper doesn't know how to serialize Java 8 date/time types.

### Stack Trace Location
```
at com.dmc.archiving.archive.ArchiveController.extractArchive(ArchiveController.java:180)
```

The issue was in this code:
```java
ObjectMapper mapper = new ObjectMapper();
String archiveJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
// ❌ LocalDateTime in exportData cannot be serialized
```

## Solution

Applied a two-part fix:

### 1. Added Jackson JSR-310 Dependency

**File:** `/pom.xml`

Added the Jackson datatype module for Java 8 date/time support:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

**Why this dependency?**
- Spring Boot includes Jackson by default, but not the JSR-310 module
- JSR-310 module provides serializers/deserializers for Java 8 date/time API
- Handles `LocalDateTime`, `LocalDate`, `LocalTime`, `ZonedDateTime`, etc.

### 2. Registered JavaTimeModule in ObjectMapper

**File:** `/src/main/java/com/dmc/archiving/archive/ArchiveController.java`

Updated the `extractArchive` method:

```java
// Convert to JSON with Java 8 date/time support
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
String archiveJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
```

**Changes explained:**
1. `registerModule(new JavaTimeModule())` - Enables Java 8 date/time serialization
2. `disable(WRITE_DATES_AS_TIMESTAMPS)` - Formats dates as ISO-8601 strings instead of timestamps

## How It Works

### Before (Error)

```java
{
  "createdAt": LocalDateTime.of(2024, 1, 15, 10, 30, 0)
}
// ❌ Jackson doesn't know how to serialize LocalDateTime
// InvalidDefinitionException thrown
```

### After (Fixed)

```java
{
  "createdAt": "2024-01-15T10:30:00"
}
// ✅ JavaTimeModule converts LocalDateTime to ISO-8601 string
// JSON serialization succeeds
```

## Date Format

The dates are now serialized as **ISO-8601 strings**:

```json
{
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-20T14:45:00.123456"
}
```

**Format:** `yyyy-MM-ddTHH:mm:ss[.SSSSSSSSS]`

### Alternative: Timestamps (Not Used)

Without disabling `WRITE_DATES_AS_TIMESTAMPS`, dates would be serialized as arrays:

```json
{
  "createdAt": [2024, 1, 15, 10, 30, 0, 0]
}
```

❌ **Not human-readable** - We use ISO-8601 strings instead

## Testing

### Test Archive Extraction

1. **Start the application:**
   ```bash
   mvn spring-boot:run
   ```

2. **Extract an archive:**
   ```bash
   curl -X POST http://localhost:2020/api/archives/1/extract \
     -H "Content-Type: application/json" \
     -d '{"password":"test"}' \
     -o archive_export.json
   ```

3. **Verify JSON output:**
   ```bash
   cat archive_export.json | jq .
   ```

4. **Check dates are formatted correctly:**
   ```json
   {
     "id": 1,
     "title": "Archive Title",
     "createdAt": "2024-01-15T10:30:00",
     "updatedAt": "2024-01-20T14:45:00",
     "standard": "NOARK5"
   }
   ```

5. ✅ **No InvalidDefinitionException!**

## What Gets Serialized

### Archive Export Data

The export includes various `LocalDateTime` fields:

```java
// From AbstractArchiveStrategy.export()
exportData.put("createdAt", archive.getCreatedAt());      // LocalDateTime
exportData.put("updatedAt", archive.getUpdatedAt());      // LocalDateTime

// From standard-specific strategies
// METS example:
metsHeader.put("createDate", archive.getCreatedAt());     // LocalDateTime
metsHeader.put("lastModDate", archive.getUpdatedAt());    // LocalDateTime
```

All of these are now properly serialized to ISO-8601 strings.

## Alternative Solutions Considered

### Option 1: @JsonFormat Annotation
```java
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
private LocalDateTime createdAt;
```
❌ **Rejected**: Would need to add annotations to Archive entity, affects GraphQL

### Option 2: Custom Serializer
```java
public class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, ...) {
        gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
```
❌ **Rejected**: More complex than using standard module

### Option 3: Convert to String Before Serialization
```java
exportData.put("createdAt", archive.getCreatedAt().toString());
```
❌ **Rejected**: Manual conversion needed everywhere, error-prone

### Option 4: JavaTimeModule (✅ Chosen)
```java
mapper.registerModule(new JavaTimeModule());
```
✅ **Selected**: Standard solution, handles all Java 8 date/time types automatically

## Spring Boot Auto-Configuration

**Note:** Spring Boot's default ObjectMapper (used by `@RestController`) automatically includes JavaTimeModule. However, when creating a **new** `ObjectMapper` instance manually (as we do in the extract endpoint), we must register the module ourselves.

### Auto-Configured (Spring MVC)
```java
@RestController
public class SomeController {
    @GetMapping("/data")
    public MyData getData() {
        return new MyData(LocalDateTime.now());
        // ✅ Works automatically - Spring's ObjectMapper has JavaTimeModule
    }
}
```

### Manual ObjectMapper (Our Case)
```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());  // ← Must register manually
```

## Related Date/Time Types

The JavaTimeModule handles all JSR-310 types:

| Type | Example Serialization |
|------|----------------------|
| `LocalDateTime` | `"2024-01-15T10:30:00"` |
| `LocalDate` | `"2024-01-15"` |
| `LocalTime` | `"10:30:00"` |
| `ZonedDateTime` | `"2024-01-15T10:30:00+01:00[Europe/Oslo]"` |
| `OffsetDateTime` | `"2024-01-15T10:30:00+01:00"` |
| `Instant` | `"2024-01-15T09:30:00Z"` |
| `Duration` | `"PT1H30M"` (ISO-8601 duration) |
| `Period` | `"P1Y2M3D"` (ISO-8601 period) |

All of these now work in archive exports.

## Benefits

### 1. **Standard Compliance** ✅
- Uses ISO-8601 date format (international standard)
- Compatible with JavaScript `new Date()` and `Date.parse()`
- Human-readable and unambiguous

### 2. **Consistent Serialization** ✅
- All Java 8 date/time types handled consistently
- Works across all archive standards (NOARK5, OAIS, PREMIS, etc.)
- No manual conversion needed

### 3. **Frontend Compatibility** ✅
```javascript
// Easy to parse in JavaScript/TypeScript
const data = await response.json();
const createdAt = new Date(data.createdAt);
console.log(createdAt); // Valid Date object
```

### 4. **Timezone Awareness** ✅
- `LocalDateTime` → No timezone (as intended)
- `ZonedDateTime` → Includes timezone
- `Instant` → UTC timestamp

## Files Modified

1. ✅ `/pom.xml` - Added `jackson-datatype-jsr310` dependency
2. ✅ `/src/main/java/com/dmc/archiving/archive/ArchiveController.java` - Registered JavaTimeModule

## Summary

✅ **Problem**: Jackson couldn't serialize `LocalDateTime` fields  
✅ **Cause**: Missing JSR-310 module and configuration  
✅ **Solution**: Added dependency + registered JavaTimeModule  
✅ **Format**: ISO-8601 strings (e.g., `"2024-01-15T10:30:00"`)  
✅ **Result**: Archive extraction works correctly  

Archive exports now include properly formatted date/time fields! 🎉
