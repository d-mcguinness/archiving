# Sample Documents Added to data.sql ✅

## Summary
Expanded the sample documents in `data.sql` from 5 to 20 documents, covering diverse file types, statuses, and associations with tenants and archives.

---

## Document Distribution

### By Tenant
- **Tenant 1 (Acme Corp)**: 6 documents (IDs: 1, 2, 6, 7, 15, 18)
- **Tenant 2 (Tech Innovations)**: 5 documents (IDs: 3, 8, 9, 16, 19)
- **Tenant 3 (Global Services)**: 5 documents (IDs: 4, 10, 11, 17, 20)
- **Tenant 4 (Startup Labs)**: 4 documents (IDs: 5, 12, 13, 14)

### By Status
- **ACTIVE**: 16 documents (most common)
- **PENDING_REVIEW**: 2 documents (IDs: 2, 15)
- **ARCHIVED**: 2 documents (IDs: 7, 16)

### By Archive Association
- **With Archive**: 13 documents (linked to specific archives)
- **Without Archive**: 7 documents (standalone documents)

---

## File Types Covered

### Documents (PDF, Word, etc.)
1. **PDF Files** (7 documents)
   - Financial reports, white papers, contracts, handbooks
   - Sizes: 1.8 MB to 15 MB

2. **Word Documents** (2 documents)
   - Annual reports, templates
   - Sizes: 800 KB to 1.5 MB

### Spreadsheets
3. **Excel Files** (3 documents)
   - Data analysis, budget breakdowns
   - Sizes: 2.5 MB to 3 MB

### Presentations
4. **PowerPoint** (3 documents)
   - Project presentations, strategy decks
   - Sizes: 5 MB to 7 MB

### Images
5. **Image Files** (2 documents)
   - PNG logo, JPEG floor plan
   - Sizes: 512 KB to 2.6 MB

### Video
6. **Video Files** (2 documents)
   - Training videos, town hall recordings
   - Sizes: 150 MB to 200 MB

### Data Files
7. **Text/CSV/JSON** (3 documents)
   - Meeting notes, catalog exports, configurations
   - Sizes: 16 KB to 1 MB

### Archives
8. **ZIP Files** (1 document)
   - Campaign assets archive
   - Size: 50 MB

---

## Sample Documents Detail

### Tenant 1 (Acme Corp) - 6 Documents

| ID | Title | Type | Size | Archive | Status |
|----|-------|------|------|---------|--------|
| 1 | Q1 Financial Summary | PDF | 2 MB | Archive 1 | ACTIVE |
| 2 | Annual Report Draft | DOCX | 1.5 MB | None | PENDING_REVIEW |
| 6 | Budget Breakdown Spreadsheet | XLSX | 2.5 MB | Archive 2 | ACTIVE |
| 7 | Old Marketing Plan | PDF | 1.8 MB | None | ARCHIVED |
| 15 | Project Template Draft | DOCX | 800 KB | Archive 10 | PENDING_REVIEW |
| 18 | Office Floor Plan | JPG | 2.6 MB | None | ACTIVE |

### Tenant 2 (Tech Innovations) - 5 Documents

| ID | Title | Type | Size | Archive | Status |
|----|-------|------|------|---------|--------|
| 3 | Research Data Analysis | XLSX | 3 MB | Archive 3 | ACTIVE |
| 8 | Research White Paper | PDF | 4 MB | Archive 4 | ACTIVE |
| 9 | Company Logo High Res | PNG | 512 KB | Archive 3 | ACTIVE |
| 16 | Campaign Assets Archive | ZIP | 50 MB | Archive 11 | ARCHIVED |
| 19 | API Configuration | JSON | 16 KB | None | ACTIVE |

### Tenant 3 (Global Services) - 5 Documents

| ID | Title | Type | Size | Archive | Status |
|----|-------|------|------|---------|--------|
| 4 | Project Presentation | PPTX | 5 MB | None | ACTIVE |
| 10 | Scanned Historical Contract | PDF | 15 MB | Archive 5 | ACTIVE |
| 11 | CEO Town Hall Recording | MP4 | 150 MB | Archive 6 | ACTIVE |
| 17 | Onboarding Training Module 1 | MP4 | 200 MB | Archive 12 | ACTIVE |
| 20 | Q2 Strategy Deck | PPTX | 7 MB | None | ACTIVE |

### Tenant 4 (Startup Labs) - 4 Documents

| ID | Title | Type | Size | Archive | Status |
|----|-------|------|------|---------|--------|
| 5 | Meeting Notes | TXT | 50 KB | None | ACTIVE |
| 12 | Product Specifications v2.0 | PDF | 3.5 MB | Archive 7 | ACTIVE |
| 13 | Employee Handbook 2026 | PDF | 2 MB | Archive 8 | ACTIVE |
| 14 | Library Catalog Export | CSV | 1 MB | Archive 9 | ACTIVE |

---

## Document-Archive Associations

Documents are properly linked to archives to demonstrate the relationship:

### Archive 1 (NOARK5 - Q1 Financial Reports)
- Document 1: Q1 Financial Summary

### Archive 2 (OAIS - Budget)
- Document 6: Budget Breakdown Spreadsheet

### Archive 3 (PREMIS - Digital Assets)
- Document 3: Research Data Analysis
- Document 9: Company Logo High Res

### Archive 4 (DUBLIN_CORE - Research Publications)
- Document 8: Research White Paper

### Archive 5 (METS - Historical Documents)
- Document 10: Scanned Historical Contract

### Archive 6 (EAD - Corporate Archives)
- Document 11: CEO Town Hall Recording

### Archive 7 (BAGIT - Product Development)
- Document 12: Product Specifications

### Archive 8 (ISADG - Organizational Records)
- Document 13: Employee Handbook

### Archive 9 (MODS - Corporate Library)
- Document 14: Library Catalog Export

### Archive 10 (NOARK5 Draft - Project Template)
- Document 15: Project Template Draft

### Archive 11 (OAIS Archived - Marketing)
- Document 16: Campaign Assets Archive

### Archive 12 (PREMIS - Training Videos)
- Document 17: Onboarding Training Module 1

---

## Key Features

### Realistic File Sizes
- Small: 16 KB - 100 KB (configs, notes)
- Medium: 1 MB - 10 MB (documents, images)
- Large: 50 MB - 200 MB (videos, archives)

### Proper File Keys
- Pattern: `tenants/{tenantId}/archives/{archiveId}/{filename}`
- Or: `tenants/{tenantId}/users/{userId}/{filename}`
- Organized by tenant and context

### Diverse Content Types
- Office documents (PDF, Word, Excel, PowerPoint)
- Images (PNG, JPEG)
- Videos (MP4)
- Data files (CSV, JSON, TXT)
- Archives (ZIP)

### Multiple Statuses
- **ACTIVE**: Currently in use
- **PENDING_REVIEW**: Awaiting approval
- **ARCHIVED**: Old, retained for history

### Temporal Data
- Most documents: Current timestamp
- Archived documents: 60-90 days old (using `INTERVAL`)

---

## Benefits

### Comprehensive Testing Data
✅ Cover all major file types
✅ Multiple document statuses
✅ Mix of archived and standalone documents
✅ Various file sizes for performance testing

### Realistic Scenarios
✅ Documents associated with archives
✅ Orphaned documents (no archive)
✅ Multi-user document uploads
✅ Cross-tenant document distribution

### Good Data Distribution
✅ Each tenant has multiple documents
✅ Each user has uploaded documents
✅ Mix of archive-linked and standalone
✅ Different content types per tenant

---

## SQL Features Used

### Conditional Inserts
```sql
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = X)
```
Prevents duplicate inserts on repeated runs

### Temporal Functions
```sql
CURRENT_TIMESTAMP - INTERVAL '90 days'
```
Creates realistic historical timestamps

### Sequence Reset
```sql
SELECT setval('documents_id_seq', ...)
```
Ensures auto-increment starts after manual inserts

---

## Testing Queries

### Get all documents
```sql
SELECT id, title, file_name, content_type, status, tenant_id, archive_id 
FROM documents 
ORDER BY id;
```

### Get documents by tenant
```sql
SELECT * FROM documents WHERE tenant_id = 1;
```

### Get documents by status
```sql
SELECT * FROM documents WHERE status = 'ACTIVE';
```

### Get documents with archive
```sql
SELECT d.*, a.title as archive_title 
FROM documents d 
JOIN archives a ON d.archive_id = a.id;
```

### Get large files (>10MB)
```sql
SELECT title, file_name, file_size / 1048576 as size_mb 
FROM documents 
WHERE file_size > 10485760 
ORDER BY file_size DESC;
```

---

## Files Modified

1. ✅ `/src/main/resources/data.sql`
   - Expanded from 5 to 20 sample documents
   - Added diverse file types
   - Added realistic metadata
   - Proper tenant and archive associations

---

## Status: ✅ COMPLETE

Successfully added 20 diverse sample documents to `data.sql` with:
- ✅ 20 documents across 4 tenants
- ✅ 8 different file types (PDF, DOCX, XLSX, PPTX, PNG, JPG, MP4, TXT, CSV, JSON, ZIP)
- ✅ 3 different statuses (ACTIVE, PENDING_REVIEW, ARCHIVED)
- ✅ 13 documents linked to archives
- ✅ Realistic file sizes and metadata
- ✅ Proper tenant and user associations

**Ready to load into database!** 🚀

