-- MIRRORS the premium_package_events backfill in src/main/resources/data.sql.
-- Kept as a separate fixture because data.sql is Postgres-targeted (setval, …) and
-- disabled in tests (spring.sql.init.mode=never). KEEP IN SYNC with data.sql.
-- (The data.sql `setval(...)` sequence reset is Postgres-specific and not exercised here.)
INSERT INTO premium_package_events (id, tenant_id, package_type, standard, generated_at)
SELECT 1, 1, 'PRESERVATION', 'NOARK5', TIMESTAMP '2026-01-15 12:00:00'
WHERE NOT EXISTS (SELECT 1 FROM premium_package_events WHERE id = 1);

INSERT INTO premium_package_events (id, tenant_id, package_type, standard, generated_at)
SELECT 2, 1, 'RELEASE', 'NOARK5', TIMESTAMP '2026-01-15 12:00:00'
WHERE NOT EXISTS (SELECT 1 FROM premium_package_events WHERE id = 2);
