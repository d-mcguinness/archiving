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
INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 1, 1, 'Q1 2026 Financial Reports', 'First quarter financial reports and analysis', 'Detailed financial analysis for Q1 2026 including revenue, expenses, and projections.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'NOARK5'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 1);

INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 2, 1, 'Annual Budget 2026', 'Complete annual budget breakdown', 'Comprehensive budget allocation across all departments for fiscal year 2026.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'OAIS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 2);

INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 3, 2, 'Project Alpha Documentation', 'Technical documentation for Project Alpha', 'Architecture diagrams, API specifications, and deployment guides for Project Alpha.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'NOARK5'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 3);

INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 4, 2, 'Research Notes - AI Initiative', 'Research and development notes for AI project', 'Experimental results, model training logs, and performance benchmarks.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAFT', 'OAIS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 4);

INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 5, 3, 'Client Service Contracts', 'Active client service agreements', 'Legal contracts and SLAs for all active client engagements.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'NOARK5'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 5);

INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 6, 3, 'Employee Handbook 2026', 'Updated employee policies and procedures', 'Company policies, benefits information, and HR procedures for 2026.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 'OAIS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 6);

INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 7, 4, 'Product Roadmap', 'Strategic product development roadmap', 'Feature prioritization and timeline for product development in 2026.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAFT', 'NOARK5'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 7);

INSERT INTO archives (id, owner_id, title, description, content, created_at, updated_at, status, standard)
SELECT 8, 5, 'Marketing Campaign Ideas', 'Brainstorming notes for marketing campaigns', 'Creative concepts and campaign strategies for Q1-Q2 2026.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAFT', 'OAIS'
WHERE NOT EXISTS (SELECT 1 FROM archives WHERE id = 8);

-- Insert User Assignments for Archives (skip duplicates)
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 1, 1, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 1 AND user_id = 1);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 1, 2, 'EDITOR', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 1 AND user_id = 2);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 2, 1, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 2 AND user_id = 1);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 3, 2, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 3 AND user_id = 2);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 3, 3, 'EDITOR', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 3 AND user_id = 3);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 4, 2, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 4 AND user_id = 2);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 4, 3, 'VIEWER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 4 AND user_id = 3);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 5, 3, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 5 AND user_id = 3);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 5, 4, 'EDITOR', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 5 AND user_id = 4);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 6, 3, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 6 AND user_id = 3);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 7, 4, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 7 AND user_id = 4);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 7, 5, 'EDITOR', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 7 AND user_id = 5);
INSERT INTO user_assignments (archive_id, user_id, role, assigned_at)
SELECT 8, 5, 'OWNER', CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM user_assignments WHERE archive_id = 8 AND user_id = 5);

