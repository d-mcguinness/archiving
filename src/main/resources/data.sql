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

-- NOARK5 (Norwegian Archives Standard) - Records Management
INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 1, 1, 'Q1 2026 Financial Reports', 'First quarter financial reports and analysis', 'Detailed financial analysis for Q1 2026 including revenue, expenses, and projections. Structured according to NOARK5 records management principles.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'NOARK5'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 1);

-- OAIS (Open Archival Information System) - Digital Preservation
INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 2, 1, 'Annual Budget 2026', 'Complete annual budget breakdown', 'Comprehensive budget allocation across all departments for fiscal year 2026. Preserved using OAIS framework for long-term digital archiving.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'OAIS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 2);

-- PREMIS (Preservation Metadata) - Digital Object Preservation
INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 3, 2, 'Digital Asset Library 2026', 'Company digital assets with preservation metadata', 'Corporate logos, brand guidelines, and digital assets archived with PREMIS metadata for preservation and rights management.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'PREMIS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 3);

-- DUBLIN_CORE - Metadata Standard for Resource Description
INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 4, 2, 'Research Publications Database', 'Academic and research publications catalog', 'Indexed research papers, white papers, and technical publications using Dublin Core metadata elements for discovery and citation.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'DUBLIN_CORE'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 4);

-- METS (Metadata Encoding and Transmission Standard) - Digital Library Objects
INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 5, 3, 'Historical Document Collection', 'Digitized historical corporate documents', 'Scanned historical documents, correspondence, and records encoded with METS structural and descriptive metadata.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'METS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 5);

-- EAD (Encoded Archival Description) - Archival Finding Aids
INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 6, 3, 'Corporate Archives Finding Aid', 'Comprehensive guide to corporate archives', 'Hierarchical description of corporate archive collections using EAD standard for archival finding aids and collection guides.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'EAD'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 6);

-- BAGIT - Packaging Format for Digital Preservation
INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 7, 4, 'Product Development Archive', 'Complete product development documentation package', 'BagIt-packaged collection of product specs, design files, and development artifacts with checksums and manifest files.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'BAGIT'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 7);

-- ISADG (International Standard Archival Description - General)
INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 8, 4, 'Organizational Records Collection', 'Multi-level archival description of organizational records', 'Corporate records described using ISADG principles including context, content, access, and related materials information.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'ISADG'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 8);

-- MODS (Metadata Object Description Schema) - Bibliographic Records
INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 9, 5, 'Corporate Library Catalog', 'Bibliographic records for corporate library', 'Books, journals, and reference materials cataloged using MODS schema for detailed bibliographic description and management.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'MODS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 9);

-- Additional archives with varied statuses
INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 10, 1, 'Project Documentation Template', 'Standard project documentation template', 'Draft template for project documentation following NOARK5 structure.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAFT', 'NOARK5'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 10);

INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 11, 2, 'Archived Marketing Materials', 'Historical marketing campaign materials', 'Past marketing campaigns preserved in OAIS format for historical reference.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ARCHIVED', 'OAIS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 11);

INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 12, 3, 'Training Video Archive', 'Employee training and onboarding videos', 'Video content with PREMIS preservation metadata and access rights.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'PREMIS'
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
