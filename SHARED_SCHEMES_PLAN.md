# Shared Scheme Definitions - Implementation Plan

## Objective
Move schemeDefinitions from frontend-only location to a shared folder accessible by both frontend and backend.

## Current Structure
```
archiving/
├── frontend/
│   └── static/
│       └── schemeDefintions/      ← Current location (typo: "Defintions")
│           ├── noark5.json
│           ├── oais.json
│           ├── ead.json
│           ├── dublincore.json
│           ├── mets.json
│           ├── premis.json
│           ├── mods.json
│           ├── bagit.json
│           └── isadg.json
└── src/
    └── main/
        └── resources/              ← Backend resources
```

## Target Structure
```
archiving/
├── shared/                         ← NEW: Shared folder
│   └── schemeDefinitions/          ← NEW: Corrected spelling
│       ├── noark5.json
│       ├── oais.json
│       ├── ead.json
│       ├── dublincore.json
│       ├── mets.json
│       ├── premis.json
│       ├── mods.json
│       ├── bagit.json
│       └── isadg.json
├── frontend/
│   └── static/
│       └── schemeDefinitions/      ← Symlink to ../../shared/schemeDefinitions
└── src/
    └── main/
        └── resources/
            └── schemeDefinitions/  ← Symlink to ../../../shared/schemeDefinitions
```

## Implementation Steps

### 1. Create Shared Directory
```bash
mkdir -p /Users/dmcg/workspace2/archiving/shared/schemeDefinitions
```

### 2. Copy All JSON Files
```bash
cp /Users/dmcg/workspace2/archiving/frontend/static/schemeDefintions/*.json \
   /Users/dmcg/workspace2/archiving/shared/schemeDefinitions/
```

### 3. Remove Old Directory
```bash
rm -rf /Users/dmcg/workspace2/archiving/frontend/static/schemeDefintions
```

### 4. Create Frontend Symlink
```bash
ln -s ../../shared/schemeDefinitions \
   /Users/dmcg/workspace2/archiving/frontend/static/schemeDefinitions
```

### 5. Create Backend Symlink
```bash
ln -s ../../../shared/schemeDefinitions \
   /Users/dmcg/workspace2/archiving/src/main/resources/schemeDefinitions
```

## Files to Move (9 total)

1. **noark5.json** (211 lines) - Norwegian Archive Standard
2. **oais.json** (283 lines) - Open Archival Information System
3. **ead.json** - Encoded Archival Description
4. **dublincore.json** - Dublin Core Metadata
5. **mets.json** - Metadata Encoding & Transmission Standard
6. **premis.json** - Preservation Metadata
7. **mods.json** - Metadata Object Description Schema
8. **bagit.json** - BagIt File Packaging Format
9. **isadg.json** - International Standard Archival Description

## Benefits

### 1. Single Source of Truth
- ✅ One location for all scheme definitions
- ✅ No duplication between frontend and backend
- ✅ Easier maintenance and updates

### 2. Backend Access
- ✅ Spring Boot can read scheme definitions
- ✅ Can validate uploads against schemas
- ✅ Can generate API responses based on schemas

### 3. Consistency
- ✅ Frontend and backend use same definitions
- ✅ No version mismatches
- ✅ Synchronized updates

### 4. Corrected Spelling
- ✅ Fixed typo: "schemeDefintions" → "schemeDefinitions"
- ✅ Consistent naming convention

## Usage

### Frontend Access
```typescript
// SvelteKit static file access
const response = await fetch('/schemeDefinitions/noark5.json');
const schema = await response.json();
```

### Backend Access
```java
// Spring Boot ResourceLoader
@Autowired
private ResourceLoader resourceLoader;

Resource resource = resourceLoader.getResource("classpath:schemeDefinitions/noark5.json");
InputStream inputStream = resource.getInputStream();
```

## Verification Commands

```bash
# Verify shared folder exists and contains files
ls -la /Users/dmcg/workspace2/archiving/shared/schemeDefinitions/

# Verify frontend symlink
ls -la /Users/dmcg/workspace2/archiving/frontend/static/schemeDefinitions

# Verify backend symlink
ls -la /Users/dmcg/workspace2/archiving/src/main/resources/schemeDefinitions

# Test frontend access (with dev server running)
curl http://localhost:5173/schemeDefinitions/noark5.json

# Count files
find /Users/dmcg/workspace2/archiving/shared/schemeDefinitions -name "*.json" | wc -l
```

## Git Considerations

### .gitignore
No changes needed - symlinks are tracked by Git

### Commit Message
```
refactor: Move scheme definitions to shared folder

- Created shared/schemeDefinitions/ directory
- Moved all 9 scheme definition JSON files to shared location
- Created symlinks from frontend/static and backend/resources
- Fixed typo: schemeDefintions → schemeDefinitions
- Enables both frontend and backend to access same schema files
```

## Rollback Plan

If issues occur:
```bash
# Remove symlinks
rm /Users/dmcg/workspace2/archiving/frontend/static/schemeDefinitions
rm /Users/dmcg/workspace2/archiving/src/main/resources/schemeDefinitions

# Restore original location
mkdir -p /Users/dmcg/workspace2/archiving/frontend/static/schemeDefintions
cp /Users/dmcg/workspace2/archiving/shared/schemeDefinitions/*.json \
   /Users/dmcg/workspace2/archiving/frontend/static/schemeDefintions/

# Remove shared folder
rm -rf /Users/dmcg/workspace2/archiving/shared
```

## Testing Checklist

- [ ] Shared folder created successfully
- [ ] All 9 JSON files copied to shared folder
- [ ] Old frontend folder removed
- [ ] Frontend symlink created
- [ ] Backend symlink created
- [ ] Frontend can access schemas via HTTP
- [ ] Backend can load schemas via ResourceLoader
- [ ] No broken links
- [ ] Git tracks changes correctly
- [ ] Dev server still serves files
- [ ] Build process works

## Status

**Status**: Ready to implement
**Date**: February 11, 2026
**Impact**: Low risk - symlinks maintain backward compatibility
