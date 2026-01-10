-- Fix PostgreSQL sequences to prevent duplicate key errors
-- Run this script if you encounter "duplicate key value violates unique constraint" errors

-- Reset users sequence
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users), true);

-- Reset tenants sequence
SELECT setval('tenants_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tenants), true);

-- Reset archives sequence
SELECT setval('archives_id_seq', (SELECT COALESCE(MAX(id), 1) FROM archives), true);

-- Reset user_assignments sequence
SELECT setval('user_assignments_id_seq', (SELECT COALESCE(MAX(id), 1) FROM user_assignments), true);

-- Reset elements sequence
SELECT setval('elements_id_seq', (SELECT COALESCE(MAX(id), 1) FROM elements), true);

-- Verify the sequences are set correctly
SELECT 'users_id_seq' as sequence_name, last_value FROM users_id_seq
UNION ALL
SELECT 'tenants_id_seq', last_value FROM tenants_id_seq
UNION ALL
SELECT 'archives_id_seq', last_value FROM archives_id_seq
UNION ALL
SELECT 'user_assignments_id_seq', last_value FROM user_assignments_id_seq
UNION ALL
SELECT 'elements_id_seq', last_value FROM elements_id_seq;

