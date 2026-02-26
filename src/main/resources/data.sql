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

-- Insert User Assignments for Archives (skip duplicates)
-- Archive 1 (NOARK5 - Financial Reports)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 1, 1, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 1 AND user_id = 1);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 1, 2, 'EDITOR', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 1 AND user_id = 2);

-- Archive 2 (OAIS - Budget)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 2, 1, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 2 AND user_id = 1);

-- Archive 3 (PREMIS - Digital Assets)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 3, 2, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 3 AND user_id = 2);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 3, 3, 'EDITOR', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 3 AND user_id = 3);

-- Archive 4 (DUBLIN_CORE - Research Publications)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 4, 2, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 4 AND user_id = 2);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 4, 3, 'VIEWER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 4 AND user_id = 3);

-- Archive 5 (METS - Historical Documents)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 5, 3, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 5 AND user_id = 3);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 5, 4, 'EDITOR', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 5 AND user_id = 4);

-- Archive 6 (EAD - Corporate Archives Finding Aid)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 6, 3, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 6 AND user_id = 3);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 6, 4, 'VIEWER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 6 AND user_id = 4);

-- Archive 7 (BAGIT - Product Development)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 7, 4, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 7 AND user_id = 4);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 7, 5, 'EDITOR', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 7 AND user_id = 5);

-- Archive 8 (ISADG - Organizational Records)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 8, 4, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 8 AND user_id = 4);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 8, 5, 'VIEWER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 8 AND user_id = 5);

-- Archive 9 (MODS - Corporate Library)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 9, 5, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 9 AND user_id = 5);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 9, 1, 'VIEWER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 9 AND user_id = 1);

-- Archive 10 (NOARK5 Draft - Project Template)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 10, 1, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 10 AND user_id = 1);

-- Archive 11 (OAIS Archived - Marketing Materials)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 11, 2, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 11 AND user_id = 2);

-- Archive 12 (PREMIS - Training Videos)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 12, 3, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 12 AND user_id = 3);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 12, 4, 'EDITOR', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 12 AND user_id = 4);

-- Reset sequences to prevent duplicate key errors
-- This ensures that auto-generated IDs start after the manually inserted IDs
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users), true);
SELECT setval('tenants_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tenants), true);
SELECT setval('archives_id_seq', (SELECT COALESCE(MAX(id), 1) FROM archives), true);
SELECT setval('user_assignments_id_seq', (SELECT COALESCE(MAX(id), 1) FROM user_assignments), true);
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

-- Reset documents sequence
SELECT setval('documents_id_seq', (SELECT COALESCE(MAX(id), 1) FROM documents), true);

