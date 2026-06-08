-- Schema migration: make archive_id nullable on elements (elements can now belong to a SIP instead)
ALTER TABLE elements ALTER COLUMN archive_id DROP NOT NULL;

-- Insert Users (skip if already exists based on email)
INSERT INTO users (name, email, age) VALUES
('John Doe', 'john.doe@example.com', 30),
('Jane Smith', 'jane.smith@example.com', 28),
('Bob Johnson', 'bob.johnson@example.com', 35),
('Alice Williams', 'alice.williams@example.com', 32),
('Charlie Brown', 'charlie.brown@example.com', 29)
ON CONFLICT (email) DO NOTHING;

-- Insert Tenants (skip if already exists based on domain)
INSERT INTO tenants (name, domain, display_name, description, status, plan, created_at, updated_at, owner_id, max_users, max_archives, max_storage_bytes, allow_external_sharing, enable_audit_log, timezone, default_language) VALUES
('Acme Corp', 'acme.example.com', 'Acme Corporation', 'Main corporate tenant for enterprise solutions', 'ACTIVE', 'ENTERPRISE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user_001', 100, 1000, 107374182400, true, true, 'America/New_York', 'en'),
('Tech Innovations', 'techinnovations.example.com', 'Tech Innovations Inc.', 'Technology division focused on R&D', 'ACTIVE', 'PROFESSIONAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user_002', 50, 500, 53687091200, true, true, 'America/Los_Angeles', 'en'),
('Global Services', 'globalservices.example.com', 'Global Services Ltd.', 'International service provider tenant', 'ACTIVE', 'BASIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user_003', 20, 200, 10737418240, false, false, 'Europe/London', 'en'),
('Startup Labs', 'startuplabs.example.com', 'Startup Labs', 'Agile startup environment', 'TRIAL', 'FREE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user_004', 10, 50, 5368709120, false, false, 'UTC', 'en')
ON CONFLICT (domain) DO NOTHING;

-- Insert User-Tenant relationships (skip duplicates)
INSERT INTO user_tenant (tenant_id, user_id) VALUES
(1, 1),  -- John Doe -> Acme Corp
(1, 2),  -- Jane Smith -> Acme Corp
(2, 2),  -- Jane Smith -> Tech Innovations
(2, 3),  -- Bob Johnson -> Tech Innovations
(3, 3),  -- Bob Johnson -> Global Services
(3, 4),  -- Alice Williams -> Global Services
(4, 4),  -- Alice Williams -> Startup Labs
(4, 5)  -- Charlie Brown -> Startup Labs
ON CONFLICT DO NOTHING;

-- Insert Archives (only if specific records don't already exist)
-- Showcasing all 9 archiving standards: NOARK5, OAIS, PREMIS, DUBLIN_CORE, METS, EAD, BAGIT, ISADG, MODS
-- NOTE: tenant_id = organization owner, owner_id = user owner

-- NOARK5 (Norwegian Archives Standard) - Records Management
-- Tenant 1 (Acme Corp), Owner: John Doe (User 1)
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 1, 1, 1, 'Q1 2026 Financial Reports', 'First quarter financial reports and analysis', 'Detailed financial analysis for Q1 2026 including revenue, expenses, and projections. Structured according to NOARK5 records management principles.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'NOARK5'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 1);

-- OAIS (Open Archival Information System) - Digital Preservation
-- Tenant 1 (Acme Corp), Owner: John Doe (User 1)
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 2, 1, 1, 'Annual Budget 2026', 'Complete annual budget breakdown', 'Comprehensive budget allocation across all departments for fiscal year 2026. Preserved using OAIS framework for long-term digital archiving.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'OAIS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 2);

-- PREMIS (Preservation Metadata) - Digital Object Preservation
-- Tenant 2 (Tech Innovations), Owner: Jane Smith (User 2)
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 3, 2, 2, 'Digital Asset Library 2026', 'Company digital assets with preservation metadata', 'Corporate logos, brand guidelines, and digital assets archived with PREMIS metadata for preservation and rights management.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'PREMIS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 3);

-- DUBLIN_CORE - Metadata Standard for Resource Description
-- Tenant 2 (Tech Innovations), Owner: Jane Smith (User 2)
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 4, 2, 2, 'Research Publications Database', 'Academic and research publications catalog', 'Indexed research papers, white papers, and technical publications using Dublin Core metadata elements for discovery and citation.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'DUBLIN_CORE'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 4);

-- METS (Metadata Encoding and Transmission Standard) - Digital Library Objects
-- Tenant 3 (Global Services), Owner: Bob Johnson (User 3)
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 5, 3, 3, 'Historical Document Collection', 'Digitized historical corporate documents', 'Scanned historical documents, correspondence, and records encoded with METS structural and descriptive metadata.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'METS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 5);

-- EAD (Encoded Archival Description) - Archival Finding Aids
-- Tenant 3 (Global Services), Owner: Bob Johnson (User 3)
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 6, 3, 3, 'Corporate Archives Finding Aid', 'Comprehensive guide to corporate archives', 'Hierarchical description of corporate archive collections using EAD standard for archival finding aids and collection guides.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'EAD'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 6);

-- BAGIT - Packaging Format for Digital Preservation
-- Tenant 4 (Startup Labs), Owner: Alice Williams (User 4)
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 7, 4, 4, 'Product Development Archive', 'Complete product development documentation package', 'BagIt-packaged collection of product specs, design files, and development artifacts with checksums and manifest files.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'BAGIT'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 7);

-- ISADG (International Standard Archival Description - General)
-- Tenant 4 (Startup Labs), Owner: Alice Williams (User 4)
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 8, 4, 4, 'Organizational Records Collection', 'Multi-level archival description of organizational records', 'Corporate records described using ISADG principles including context, content, access, and related materials information.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'ISADG'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 8);

-- MODS (Metadata Object Description Schema) - Bibliographic Records
-- Tenant 4 (Startup Labs), Owner: Charlie Brown (User 5)
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 9, 4, 5, 'Corporate Library Catalog', 'Bibliographic records for corporate library', 'Books, journals, and reference materials cataloged using MODS schema for detailed bibliographic description and management.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'MODS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 9);

-- Additional archives with varied statuses
-- Tenant 1 (Acme Corp), Owner: John Doe (User 1) - DRAFT
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 10, 1, 1, 'Project Documentation Template', 'Standard project documentation template', 'Draft template for project documentation following NOARK5 structure.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAFT', 'NOARK5'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 10);

-- Tenant 2 (Tech Innovations), Owner: Jane Smith (User 2) - ARCHIVED
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 11, 2, 2, 'Archived Marketing Materials', 'Historical marketing campaign materials', 'Past marketing campaigns preserved in OAIS format for historical reference.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ARCHIVED', 'OAIS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 11);

-- Tenant 3 (Global Services), Owner: Bob Johnson (User 3)
INSERT INTO archives (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 12, 3, 3, 'Training Video Archive', 'Employee training and onboarding videos', 'Video content with PREMIS preservation metadata and access rights.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'PREMIS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 12);

-- Insert Archive-User assignments (skip duplicates)
INSERT INTO archive_users (archive_id, user_id) VALUES (1, 1) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (1, 2) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (2, 1) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (3, 2) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (3, 3) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (4, 2) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (4, 3) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (5, 3) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (5, 4) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (6, 3) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (6, 4) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (7, 4) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (7, 5) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (8, 4) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (8, 5) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (9, 5) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (9, 1) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (10, 1) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (11, 2) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (12, 3) ON CONFLICT DO NOTHING;
INSERT INTO archive_users (archive_id, user_id) VALUES (12, 4) ON CONFLICT DO NOTHING;

-- Reset sequences to prevent duplicate key errors
-- This ensures that auto-generated IDs start after the manually inserted IDs
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users), true);
SELECT setval('tenants_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tenants), true);
SELECT setval('archives_id_seq', (SELECT COALESCE(MAX(id), 1) FROM archives), true);
SELECT setval('elements_id_seq', (SELECT COALESCE(MAX(id), 1) FROM elements), true);

-- Insert sample documents
-- NOTE: tenant_id = organization owner, user_id = uploader, archive_id = associated archive (optional)

-- Document 1 - User 1, Tenant 1, Archive 1 (Financial Reports)
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 1, 'Q1 Financial Summary', 'Summary of Q1 2026 financial results for Acme Corporation', 'q1-financial-summary.pdf', 'tenants/1/archives/1/q1-financial-summary.pdf', 'https://s3.amazonaws.com/archiving-uploads/sample.pdf', 2048576, 'application/pdf', 1, 1, 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 1);

-- Document 2 - User 2, Tenant 1, No Archive
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 2, 'Annual Report Draft', 'Draft version of 2026 annual report awaiting review', 'annual-report-draft.docx', 'tenants/1/users/2/annual-report-draft.docx', 'https://s3.amazonaws.com/archiving-uploads/sample.docx', 1536000, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 2, 1, NULL, 'PENDING_REVIEW', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 2);

-- Document 3 - User 3, Tenant 2, Archive 3 (Digital Assets)
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 3, 'Research Data Analysis', 'Statistical analysis of research data for Tech Innovations', 'research-analysis.xlsx', 'tenants/2/archives/3/research-analysis.xlsx', 'https://s3.amazonaws.com/archiving-uploads/sample.xlsx', 3145728, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 3, 2, 3, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 3);

-- Document 4 - User 4, Tenant 3, No Archive
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 4, 'Project Presentation', 'Quarterly project status presentation for Global Services', 'project-presentation.pptx', 'tenants/3/users/4/project-presentation.pptx', 'https://s3.amazonaws.com/archiving-uploads/sample.pptx', 5242880, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 4, 3, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 4);

-- Document 5 - User 5, Tenant 4, No Archive
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 5, 'Meeting Notes', 'Weekly team meeting notes and action items', 'meeting-notes.txt', 'tenants/4/users/5/meeting-notes.txt', 'https://s3.amazonaws.com/archiving-uploads/sample.txt', 51200, 'text/plain', 5, 4, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 5);

-- Document 6 - User 1, Tenant 1, Archive 2 (Budget)
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 6, 'Budget Breakdown Spreadsheet', 'Detailed budget breakdown with department allocations', 'budget-2026-breakdown.xlsx', 'tenants/1/archives/2/budget-2026-breakdown.xlsx', 'https://s3.amazonaws.com/archiving-uploads/sample.xlsx', 2621440, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 1, 1, 2, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 6);

-- Document 7 - User 2, Tenant 1, No Archive - ARCHIVED status
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 7, 'Old Marketing Plan', 'Archived marketing plan from 2025 - no longer active', 'marketing-plan-2025.pdf', 'tenants/1/users/2/marketing-plan-2025.pdf', 'https://s3.amazonaws.com/archiving-uploads/sample.pdf', 1843200, 'application/pdf', 2, 1, NULL, 'ARCHIVED', CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP - INTERVAL '90 days'
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 7);

-- Document 8 - User 3, Tenant 2, Archive 4 (Research Publications)
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 8, 'Research White Paper', 'Technical white paper on machine learning applications', 'ml-applications-whitepaper.pdf', 'tenants/2/archives/4/ml-applications-whitepaper.pdf', 'https://s3.amazonaws.com/archiving-uploads/sample.pdf', 4194304, 'application/pdf', 3, 2, 4, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 8);

-- Document 9 - User 2, Tenant 2, Archive 3 (Digital Assets) - Image
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 9, 'Company Logo High Res', 'High resolution company logo for print materials', 'tech-innovations-logo.png', 'tenants/2/archives/3/tech-innovations-logo.png', 'https://s3.amazonaws.com/archiving-uploads/sample.png', 524288, 'image/png', 2, 2, 3, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 9);

-- Document 10 - User 4, Tenant 3, Archive 5 (Historical Documents)
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 10, 'Scanned Historical Contract', 'Digitized contract from 1995 - historical reference', 'contract-1995-scanned.pdf', 'tenants/3/archives/5/contract-1995-scanned.pdf', 'https://s3.amazonaws.com/archiving-uploads/sample.pdf', 15728640, 'application/pdf', 4, 3, 5, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 10);

-- Document 11 - User 3, Tenant 3, Archive 6 (Corporate Archives) - Video
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 11, 'CEO Town Hall Recording', 'Recording of quarterly CEO town hall meeting', 'ceo-townhall-q1-2026.mp4', 'tenants/3/archives/6/ceo-townhall-q1-2026.mp4', 'https://s3.amazonaws.com/archiving-uploads/sample.mp4', 157286400, 'video/mp4', 3, 3, 6, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 11);

-- Document 12 - User 5, Tenant 4, Archive 7 (Product Development)
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 12, 'Product Specifications v2.0', 'Updated product specifications document', 'product-specs-v2.0.pdf', 'tenants/4/archives/7/product-specs-v2.0.pdf', 'https://s3.amazonaws.com/archiving-uploads/sample.pdf', 3670016, 'application/pdf', 5, 4, 7, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 12);

-- Document 13 - User 4, Tenant 4, Archive 8 (Organizational Records)
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 13, 'Employee Handbook 2026', 'Updated employee handbook with new policies', 'employee-handbook-2026.pdf', 'tenants/4/archives/8/employee-handbook-2026.pdf', 'https://s3.amazonaws.com/archiving-uploads/sample.pdf', 2097152, 'application/pdf', 4, 4, 8, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 13);

-- Document 14 - User 5, Tenant 4, Archive 9 (Corporate Library) - CSV
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 14, 'Library Catalog Export', 'Complete library catalog in CSV format', 'library-catalog-export.csv', 'tenants/4/archives/9/library-catalog-export.csv', 'https://s3.amazonaws.com/archiving-uploads/sample.csv', 1048576, 'text/csv', 5, 4, 9, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 14);

-- Document 15 - User 1, Tenant 1, Archive 10 (Draft Template) - PENDING_REVIEW
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 15, 'Project Template Draft', 'Draft project documentation template needs review', 'project-template-draft.docx', 'tenants/1/archives/10/project-template-draft.docx', 'https://s3.amazonaws.com/archiving-uploads/sample.docx', 819200, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 1, 1, 10, 'PENDING_REVIEW', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 15);

-- Document 16 - User 2, Tenant 2, Archive 11 (Archived Marketing) - ARCHIVED
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 16, 'Campaign Assets Archive', 'ZIP archive of old campaign assets', 'campaign-2025-assets.zip', 'tenants/2/archives/11/campaign-2025-assets.zip', 'https://s3.amazonaws.com/archiving-uploads/sample.zip', 52428800, 'application/zip', 2, 2, 11, 'ARCHIVED', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 16);

-- Document 17 - User 3, Tenant 3, Archive 12 (Training Videos) - Video
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 17, 'Onboarding Training Module 1', 'First module of employee onboarding training series', 'onboarding-module-1.mp4', 'tenants/3/archives/12/onboarding-module-1.mp4', 'https://s3.amazonaws.com/archiving-uploads/sample.mp4', 209715200, 'video/mp4', 3, 3, 12, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 17);

-- Document 18 - User 1, Tenant 1, No Archive - Image
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 18, 'Office Floor Plan', 'Updated office floor plan with new seating arrangements', 'office-floor-plan-2026.jpg', 'tenants/1/users/1/office-floor-plan-2026.jpg', 'https://s3.amazonaws.com/archiving-uploads/sample.jpg', 2621440, 'image/jpeg', 1, 1, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 18);

-- Document 19 - User 3, Tenant 2, No Archive - JSON data file
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 19, 'API Configuration', 'API configuration settings in JSON format', 'api-config.json', 'tenants/2/users/3/api-config.json', 'https://s3.amazonaws.com/archiving-uploads/sample.json', 16384, 'application/json', 3, 2, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 19);

-- Document 20 - User 4, Tenant 3, No Archive - Presentation
INSERT INTO documents (id, title, description, file_name, file_key, file_url, file_size, content_type, user_id, tenant_id, archive_id, status, created_at, uploaded_at)
SELECT 20, 'Q2 Strategy Deck', 'Strategic planning presentation for Q2 2026', 'q2-strategy-deck.pptx', 'tenants/3/users/4/q2-strategy-deck.pptx', 'https://s3.amazonaws.com/archiving-uploads/sample.pptx', 7340032, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 4, 3, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE id = 20);

-- Associate some documents with SIPs
UPDATE documents SET sip_id = 1 WHERE id = 2 AND sip_id IS NULL;  -- Annual Report Draft -> NOARK5 Personnel Records SIP
UPDATE documents SET sip_id = 3 WHERE id = 4 AND sip_id IS NULL;  -- Project Presentation -> PREMIS Software Artifacts SIP
UPDATE documents SET sip_id = 7 WHERE id = 5 AND sip_id IS NULL;  -- Meeting Notes -> BagIt Source Code Package SIP

-- Reset documents sequence
SELECT setval('documents_id_seq', (SELECT COALESCE(MAX(id), 1) FROM documents), true);

-- Insert SIPs (Submission Information Packages)
-- SIP 1 - NOARK5, Tenant 1 (Acme Corp), Owner: John Doe (User 1)
INSERT INTO sips (id, tenant_id, owner_id, archive_id, title, description, content, created_at, updated_at, status, standard)
SELECT 1, 1, 1, 1, 'NOARK5 Personnel Records SIP', 'Submission package for personnel records following NOARK5 standard', '{"sipType":"Archive (Arkiv)","standard":"NOARK5","entity":"Archive","fields":{"systemID":"SIP-NOARK5-001","title":"Personnel Records","archiveStatus":"Created","documentMedium":"Electronic archive"}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAFT', 'NOARK5'
WHERE NOT EXISTS (SELECT 1 FROM sips WHERE id = 1);

-- SIP 2 - OAIS, Tenant 1 (Acme Corp), Owner: Jane Smith (User 2)
INSERT INTO sips (id, tenant_id, owner_id, archive_id, title, description, content, created_at, updated_at, status, standard)
SELECT 2, 1, 2, 2, 'OAIS Digital Preservation Package', 'OAIS-compliant submission information package for digital preservation', '{"sipType":"Submission Information Package","standard":"OAIS","entity":"Submission Information Package","fields":{"packageID":"SIP-OAIS-001","title":"Digital Preservation Package","packageType":"SIP","producer":"Jane Smith"}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SUBMITTED', 'OAIS'
WHERE NOT EXISTS (SELECT 1 FROM sips WHERE id = 2);

-- SIP 3 - PREMIS, Tenant 2 (Tech Innovations), Owner: Bob Johnson (User 3)
INSERT INTO sips (id, tenant_id, owner_id, archive_id, title, description, content, created_at, updated_at, status, standard)
SELECT 3, 2, 3, 3, 'PREMIS Software Artifacts SIP', 'Preservation metadata package for software development artifacts', '{"sipType":"Preservation Object","standard":"PREMIS","entity":"Object","fields":{"objectIdentifierType":"local","objectIdentifierValue":"SIP-PREMIS-001","objectCategory":"Representation","originalName":"Software Artifacts"}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'VALIDATED', 'PREMIS'
WHERE NOT EXISTS (SELECT 1 FROM sips WHERE id = 3);

-- SIP 4 - DUBLIN_CORE, Tenant 2 (Tech Innovations), Owner: Jane Smith (User 2)
INSERT INTO sips (id, tenant_id, owner_id, archive_id, title, description, content, created_at, updated_at, status, standard)
SELECT 4, 2, 2, 4, 'Dublin Core Research Dataset', 'Research publications described using Dublin Core metadata', '{"sipType":"Resource","standard":"Dublin Core","entity":"Resource","fields":{"resourceIdentifier":"SIP-DC-001","resourceType":"Dataset"}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAFT', 'DUBLIN_CORE'
WHERE NOT EXISTS (SELECT 1 FROM sips WHERE id = 4);

-- SIP 5 - METS, Tenant 3 (Global Services), Owner: Bob Johnson (User 3)
INSERT INTO sips (id, tenant_id, owner_id, archive_id, title, description, content, created_at, updated_at, status, standard)
SELECT 5, 3, 3, 5, 'METS Digitized Documents Package', 'METS-encoded package of digitized historical documents', '{"sipType":"METS Document","standard":"METS","entity":"METS Document","fields":{"metsID":"SIP-METS-001","label":"Digitized Documents","type":"digital object"}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACCEPTED', 'METS'
WHERE NOT EXISTS (SELECT 1 FROM sips WHERE id = 5);

-- SIP 6 - EAD, Tenant 3 (Global Services), Owner: Alice Williams (User 4)
INSERT INTO sips (id, tenant_id, owner_id, archive_id, title, description, content, created_at, updated_at, status, standard)
SELECT 6, 3, 4, 6, 'EAD Finding Aid SIP', 'Encoded Archival Description finding aid submission', '{"sipType":"Finding Aid (EAD)","standard":"EAD","entity":"EAD","fields":{"eadID":"SIP-EAD-001","audience":"external","lang":"eng"}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAFT', 'EAD'
WHERE NOT EXISTS (SELECT 1 FROM sips WHERE id = 6);

-- SIP 7 - BAGIT, Tenant 4 (Startup Labs), Owner: Alice Williams (User 4)
INSERT INTO sips (id, tenant_id, owner_id, archive_id, title, description, content, created_at, updated_at, status, standard)
SELECT 7, 4, 4, 7, 'BagIt Source Code Package', 'BagIt-packaged source code and documentation', '{"sipType":"Bag","standard":"BagIt","entity":"Bag","fields":{"bagName":"source-code-bag","payloadOxum":"1024.5","bagSize":"1 GB","isComplete":"true","isValid":"true"}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SUBMITTED', 'BAGIT'
WHERE NOT EXISTS (SELECT 1 FROM sips WHERE id = 7);

-- SIP 8 - ISADG, Tenant 4 (Startup Labs), Owner: Charlie Brown (User 5)
INSERT INTO sips (id, tenant_id, owner_id, archive_id, title, description, content, created_at, updated_at, status, standard)
SELECT 8, 4, 5, 8, 'ISAD(G) Founders Archive', 'Multi-level archival description of founders documents', '{"sipType":"Archival Description","standard":"ISAD(G)","entity":"Archival Description","fields":{"descriptionID":"SIP-ISADG-001","levelOfDescription":"Fonds"}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAFT', 'ISADG'
WHERE NOT EXISTS (SELECT 1 FROM sips WHERE id = 8);

-- SIP 9 - MODS, Tenant 4 (Startup Labs), Owner: Charlie Brown (User 5)
INSERT INTO sips (id, tenant_id, owner_id, archive_id, title, description, content, created_at, updated_at, status, standard)
SELECT 9, 4, 5, 9, 'MODS Technical Library SIP', 'Bibliographic records for technical reference library', '{"sipType":"MODS Record","standard":"MODS","entity":"MODS","fields":{"modsID":"SIP-MODS-001","version":"3.8"}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'VALIDATED', 'MODS'
WHERE NOT EXISTS (SELECT 1 FROM sips WHERE id = 9);

-- Insert Elements for SIPs (root elements)
-- Element for SIP 1 (NOARK5)
INSERT INTO elements (id, element_identifier, entity_name, entity_type, norwegian_name, english_name, title, description, created_at, created_by, status, is_root)
SELECT 1000, 'SIP-NOARK5-001', 'Archive', 'archive', 'Arkiv', 'Archive', 'Personnel Records', 'Root element for personnel records SIP', CURRENT_TIMESTAMP, '1', 'Opprettet', true
WHERE NOT EXISTS (SELECT 1 FROM elements WHERE id = 1000);

-- Element for SIP 2 (OAIS)
INSERT INTO elements (id, element_identifier, entity_name, entity_type, english_name, title, description, created_at, created_by, status, is_root)
SELECT 1001, 'SIP-OAIS-001', 'Submission Information Package', 'sip', 'Submission Information Package', 'Digital Preservation Package', 'Root element for OAIS digital preservation SIP', CURRENT_TIMESTAMP, '2', 'Opprettet', true
WHERE NOT EXISTS (SELECT 1 FROM elements WHERE id = 1001);

-- Element for SIP 3 (PREMIS)
INSERT INTO elements (id, element_identifier, entity_name, entity_type, english_name, title, description, created_at, created_by, status, is_root)
SELECT 1002, 'SIP-PREMIS-001', 'Object', 'object', 'Preservation Object', 'Software Artifacts', 'Root element for PREMIS preservation SIP', CURRENT_TIMESTAMP, '3', 'Opprettet', true
WHERE NOT EXISTS (SELECT 1 FROM elements WHERE id = 1002);

-- Element for SIP 4 (DUBLIN_CORE)
INSERT INTO elements (id, element_identifier, entity_name, entity_type, english_name, title, description, created_at, created_by, status, is_root)
SELECT 1003, 'SIP-DC-001', 'Resource', 'resource', 'Resource', 'Research Dataset', 'Root element for Dublin Core research dataset SIP', CURRENT_TIMESTAMP, '2', 'Opprettet', true
WHERE NOT EXISTS (SELECT 1 FROM elements WHERE id = 1003);

-- Element for SIP 5 (METS)
INSERT INTO elements (id, element_identifier, entity_name, entity_type, english_name, title, description, created_at, created_by, status, is_root)
SELECT 1004, 'SIP-METS-001', 'METS Document', 'metsDocument', 'METS Document', 'Digitized Documents', 'Root element for METS digitized documents SIP', CURRENT_TIMESTAMP, '3', 'Opprettet', true
WHERE NOT EXISTS (SELECT 1 FROM elements WHERE id = 1004);

-- Element for SIP 6 (EAD)
INSERT INTO elements (id, element_identifier, entity_name, entity_type, english_name, title, description, created_at, created_by, status, is_root)
SELECT 1005, 'SIP-EAD-001', 'EAD', 'findingAid', 'Finding Aid', 'Finding Aid SIP', 'Root element for EAD finding aid SIP', CURRENT_TIMESTAMP, '4', 'Opprettet', true
WHERE NOT EXISTS (SELECT 1 FROM elements WHERE id = 1005);

-- Element for SIP 7 (BAGIT)
INSERT INTO elements (id, element_identifier, entity_name, entity_type, english_name, title, description, created_at, created_by, status, is_root)
SELECT 1006, 'source-code-bag', 'Bag', 'bag', 'Bag', 'Source Code Package', 'Root element for BagIt source code SIP', CURRENT_TIMESTAMP, '4', 'Opprettet', true
WHERE NOT EXISTS (SELECT 1 FROM elements WHERE id = 1006);

-- Element for SIP 8 (ISADG)
INSERT INTO elements (id, element_identifier, entity_name, entity_type, english_name, title, description, created_at, created_by, status, is_root)
SELECT 1007, 'SIP-ISADG-001', 'Archival Description', 'fonds', 'Archival Description', 'Founders Archive', 'Root element for ISAD(G) founders archive SIP', CURRENT_TIMESTAMP, '5', 'Opprettet', true
WHERE NOT EXISTS (SELECT 1 FROM elements WHERE id = 1007);

-- Element for SIP 9 (MODS)
INSERT INTO elements (id, element_identifier, entity_name, entity_type, english_name, title, description, created_at, created_by, status, is_root)
SELECT 1008, 'SIP-MODS-001', 'MODS', 'bibliographic', 'MODS Record', 'Technical Library', 'Root element for MODS technical library SIP', CURRENT_TIMESTAMP, '5', 'Opprettet', true
WHERE NOT EXISTS (SELECT 1 FROM elements WHERE id = 1008);

-- Set root elements on SIPs
UPDATE sips SET root_element_id = 1000 WHERE id = 1 AND root_element_id IS NULL;
UPDATE sips SET root_element_id = 1001 WHERE id = 2 AND root_element_id IS NULL;
UPDATE sips SET root_element_id = 1002 WHERE id = 3 AND root_element_id IS NULL;
UPDATE sips SET root_element_id = 1003 WHERE id = 4 AND root_element_id IS NULL;
UPDATE sips SET root_element_id = 1004 WHERE id = 5 AND root_element_id IS NULL;
UPDATE sips SET root_element_id = 1005 WHERE id = 6 AND root_element_id IS NULL;
UPDATE sips SET root_element_id = 1006 WHERE id = 7 AND root_element_id IS NULL;
UPDATE sips SET root_element_id = 1007 WHERE id = 8 AND root_element_id IS NULL;
UPDATE sips SET root_element_id = 1008 WHERE id = 9 AND root_element_id IS NULL;

-- Backfill archive_id for existing SIPs (match by tenant + standard)
UPDATE sips SET archive_id = 1 WHERE id = 1 AND archive_id IS NULL;
UPDATE sips SET archive_id = 2 WHERE id = 2 AND archive_id IS NULL;
UPDATE sips SET archive_id = 3 WHERE id = 3 AND archive_id IS NULL;
UPDATE sips SET archive_id = 4 WHERE id = 4 AND archive_id IS NULL;
UPDATE sips SET archive_id = 5 WHERE id = 5 AND archive_id IS NULL;
UPDATE sips SET archive_id = 6 WHERE id = 6 AND archive_id IS NULL;
UPDATE sips SET archive_id = 7 WHERE id = 7 AND archive_id IS NULL;
UPDATE sips SET archive_id = 8 WHERE id = 8 AND archive_id IS NULL;
UPDATE sips SET archive_id = 9 WHERE id = 9 AND archive_id IS NULL;

-- Catch-all: assign any remaining SIPs with null archive_id to a matching archive (by tenant + standard)
UPDATE sips s SET archive_id = (
    SELECT a.id FROM archives a
    WHERE a.tenant_id = s.tenant_id AND a.standard = s.standard
    ORDER BY a.id LIMIT 1
) WHERE s.archive_id IS NULL AND EXISTS (
    SELECT 1 FROM archives a WHERE a.tenant_id = s.tenant_id AND a.standard = s.standard
);

-- For any SIPs still without archive_id (no matching archive), assign to the first archive in the same tenant
UPDATE sips s SET archive_id = (
    SELECT a.id FROM archives a
    WHERE a.tenant_id = s.tenant_id
    ORDER BY a.id LIMIT 1
) WHERE s.archive_id IS NULL AND EXISTS (
    SELECT 1 FROM archives a WHERE a.tenant_id = s.tenant_id
);

-- Insert Fields for SIP root elements
-- Fields for SIP 1 (NOARK5)
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2000, 1000, 'systemID', 'System ID', 'string', 'SIP-NOARK5-001'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2000);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2001, 1000, 'title', 'Title', 'string', 'Personnel Records'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2001);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2002, 1000, 'archiveStatus', 'Archive Status', 'string', 'Created'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2002);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2003, 1000, 'documentMedium', 'Document Medium', 'string', 'Electronic archive'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2003);

-- Fields for SIP 2 (OAIS)
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2010, 1001, 'packageID', 'Package ID', 'string', 'SIP-OAIS-001'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2010);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2011, 1001, 'packageType', 'Package Type', 'string', 'SIP'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2011);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2012, 1001, 'producer', 'Producer', 'string', 'Jane Smith'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2012);

-- Fields for SIP 3 (PREMIS)
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2020, 1002, 'objectIdentifierType', 'Object Identifier Type', 'string', 'local'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2020);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2021, 1002, 'objectIdentifierValue', 'Object Identifier Value', 'string', 'SIP-PREMIS-001'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2021);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2022, 1002, 'objectCategory', 'Object Category', 'string', 'Representation'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2022);

-- Fields for SIP 4 (DUBLIN_CORE)
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2030, 1003, 'resourceIdentifier', 'Resource Identifier', 'string', 'SIP-DC-001'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2030);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2031, 1003, 'resourceType', 'Resource Type', 'string', 'Dataset'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2031);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2032, 1003, 'language', 'Language', 'string', 'en'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2032);

-- Fields for SIP 5 (METS)
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2040, 1004, 'metsID', 'METS ID', 'string', 'SIP-METS-001'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2040);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2041, 1004, 'label', 'Label', 'string', 'Digitized Documents'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2041);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2042, 1004, 'type', 'Type', 'string', 'digital object'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2042);

-- Fields for SIP 6 (EAD)
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2050, 1005, 'eadID', 'EAD ID', 'string', 'SIP-EAD-001'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2050);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2051, 1005, 'audience', 'Audience', 'string', 'external'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2051);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2052, 1005, 'lang', 'Language', 'string', 'eng'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2052);

-- Fields for SIP 7 (BAGIT)
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2060, 1006, 'bagName', 'Bag Name', 'string', 'source-code-bag'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2060);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2061, 1006, 'payloadOxum', 'Payload Oxum', 'string', '1024.5'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2061);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2062, 1006, 'bagSize', 'Bag Size', 'string', '1 GB'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2062);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2063, 1006, 'isComplete', 'Is Complete', 'string', 'true'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2063);

-- Fields for SIP 8 (ISADG)
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2070, 1007, 'descriptionID', 'Description ID', 'string', 'SIP-ISADG-001'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2070);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2071, 1007, 'levelOfDescription', 'Level of Description', 'string', 'Fonds'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2071);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2072, 1007, 'repositoryCode', 'Repository Code', 'string', 'SL-ARCH'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2072);

-- Fields for SIP 9 (MODS)
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2080, 1008, 'modsID', 'MODS ID', 'string', 'SIP-MODS-001'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2080);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2081, 1008, 'version', 'Version', 'string', '3.8'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2081);
INSERT INTO fields (id, element_id, name, label, type, value)
SELECT 2082, 1008, 'genre', 'Genre', 'string', 'technical reference'
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id = 2082);

-- Insert SIP-User assignments
INSERT INTO sip_users (sip_id, user_id) VALUES (1, 1) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (1, 2) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (2, 2) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (3, 3) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (3, 2) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (4, 2) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (5, 3) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (5, 4) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (6, 4) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (7, 4) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (7, 5) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (8, 5) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (9, 5) ON CONFLICT DO NOTHING;
INSERT INTO sip_users (sip_id, user_id) VALUES (9, 4) ON CONFLICT DO NOTHING;

-- Reset SIP-related sequences
SELECT setval('sips_id_seq', (SELECT COALESCE(MAX(id), 1) FROM sips), true);
SELECT setval('elements_id_seq', (SELECT COALESCE(MAX(id), 1) FROM elements), true);
SELECT setval('fields_id_seq', (SELECT COALESCE(MAX(id), 1) FROM fields), true);

-- Insert AIPs (Archival Information Packages)
-- AIP 1 - NOARK5, derived from SIP 1. Demonstrates the SIP -> AIP step in the
-- archival lifecycle for the Noark 5 chain (matched by the Noark5 arkivuttrekk
-- XML generators).
INSERT INTO aips (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard, source_sip_id)
SELECT 1, 1, 1, 'NOARK5 Personnel AIP', 'Preserved personnel records derived from SIP 1', '{"aipType":"Archival Information Package","standard":"NOARK5","sourceSipId":1}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'STORED', 'NOARK5', 1
WHERE NOT EXISTS (SELECT 1 FROM aips WHERE id = 1);

-- Element for AIP 1 (NOARK5)
INSERT INTO elements (id, element_identifier, entity_name, entity_type, norwegian_name, english_name, title, description, created_at, created_by, status, is_root)
SELECT 1100, 'AIP-NOARK5-001', 'Personnel', 'Arkivdel', 'Arkiv', 'Archive', 'Personnel Records', 'Preserved personnel records', CURRENT_TIMESTAMP, 'david', 'Bevart', true
WHERE NOT EXISTS (SELECT 1 FROM elements WHERE id = 1100);

UPDATE aips SET root_element_id = 1100 WHERE id = 1 AND root_element_id IS NULL;

-- AIP-User assignments
INSERT INTO aip_users (aip_id, user_id) VALUES (1, 1) ON CONFLICT DO NOTHING;

-- Insert DIPs (Dissemination Information Packages)
-- DIP 1 - NOARK5, derived from AIP 1. Completes the SIP -> AIP -> DIP chain.
INSERT INTO dips (id, tenant_id, owner_id, title, description, content, created_at, updated_at, status, standard, source_aip_id)
SELECT 1, 1, 1, 'NOARK5 Personnel DIP', 'Delivered personnel records derived from AIP 1', '{"dipType":"Dissemination Information Package","standard":"NOARK5","sourceAipId":1}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DISSEMINATED', 'NOARK5', 1
WHERE NOT EXISTS (SELECT 1 FROM dips WHERE id = 1);

-- Element for DIP 1 (NOARK5)
INSERT INTO elements (id, element_identifier, entity_name, entity_type, norwegian_name, english_name, title, description, created_at, created_by, status, is_root)
SELECT 1200, 'DIP-NOARK5-001', 'Personnel', 'Arkivdel', 'Arkiv', 'Archive', 'Personnel Records', 'Delivered to consumer', CURRENT_TIMESTAMP, 'david', 'Avlevert', true
WHERE NOT EXISTS (SELECT 1 FROM elements WHERE id = 1200);

UPDATE dips SET root_element_id = 1200 WHERE id = 1 AND root_element_id IS NULL;

-- DIP-User assignments
INSERT INTO dip_users (dip_id, user_id) VALUES (1, 1) ON CONFLICT DO NOTHING;

-- Premium-package event ledger backfill.
-- The seeded billable NOARK5 AIP 1 and DIP 1 (tenant 1) are premium generation
-- events. The metering meter reads the append-only premium_package_events
-- ledger, so mirror the seeded SIP -> AIP -> DIP chain here; otherwise a fresh
-- ledger would report 0 premium packages for the seeded tenant.
INSERT INTO premium_package_events (id, tenant_id, package_type, standard, generated_at)
SELECT 1, 1, 'AIP', 'NOARK5', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM premium_package_events WHERE id = 1);

INSERT INTO premium_package_events (id, tenant_id, package_type, standard, generated_at)
SELECT 2, 1, 'DIP', 'NOARK5', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM premium_package_events WHERE id = 2);

-- Reset AIP/DIP sequences
SELECT setval('aips_id_seq', (SELECT COALESCE(MAX(id), 1) FROM aips), true);
SELECT setval('dips_id_seq', (SELECT COALESCE(MAX(id), 1) FROM dips), true);
SELECT setval('elements_id_seq', (SELECT COALESCE(MAX(id), 1) FROM elements), true);
SELECT setval('premium_package_events_id_seq', (SELECT COALESCE(MAX(id), 1) FROM premium_package_events), true);

