-- V5: Rename the PRODUCT package vocabulary SIP/AIP/DIP -> Intake/Preservation/Release.
--
-- This is a customer-facing vocabulary rename. OAIS terminology is retained only as a
-- documented standard mapping: NOARK5 XML output (uttrekkType) and the OAIS/E-ARK
-- strategy metadata still emit SIP/AIP/DIP, and the per-stage S3 storage prefixes are
-- unchanged — so stored objects and standards-conformant output are unaffected.
--
-- packages.stage and premium_package_events.package_type are @Enumerated(EnumType.STRING):
-- the stored value is the Java enum constant name, hence UPPERCASE. 'PRESERVATION' is 12
-- chars, so both columns are widened (10/8 -> 20).

-- 1) packages.stage : widen + remap + swap CHECK ------------------------------
ALTER TABLE packages DROP CONSTRAINT packages_stage_check;
ALTER TABLE packages ALTER COLUMN stage TYPE varchar(20);
UPDATE packages SET stage = 'INTAKE'       WHERE stage = 'SIP';
UPDATE packages SET stage = 'PRESERVATION' WHERE stage = 'AIP';
UPDATE packages SET stage = 'RELEASE'      WHERE stage = 'DIP';
ALTER TABLE packages
    ADD CONSTRAINT packages_stage_check CHECK (stage IN ('INTAKE','PRESERVATION','RELEASE'));

-- 2) premium_package_events.package_type : widen + remap + swap CHECK ----------
ALTER TABLE premium_package_events DROP CONSTRAINT premium_package_events_package_type_check;
ALTER TABLE premium_package_events ALTER COLUMN package_type TYPE varchar(20);
UPDATE premium_package_events SET package_type = 'PRESERVATION' WHERE package_type = 'AIP';
UPDATE premium_package_events SET package_type = 'RELEASE'      WHERE package_type = 'DIP';
ALTER TABLE premium_package_events
    ADD CONSTRAINT premium_package_events_package_type_check
    CHECK (package_type IN ('PRESERVATION','RELEASE'));

-- 3) Rename the legacy per-stage tables, their join tables, and FK columns -----
--    (Postgres carries indexes and foreign keys across RENAME automatically.)

-- SIP -> Intake
ALTER TABLE sip_users RENAME COLUMN sip_id TO intake_id;
ALTER TABLE sip_users RENAME TO intake_users;
ALTER TABLE sips       RENAME TO intakes;
ALTER TABLE documents  RENAME COLUMN sip_id TO intake_id;

-- AIP -> Preservation
ALTER TABLE aip_users RENAME COLUMN aip_id TO preservation_id;
ALTER TABLE aip_users RENAME TO preservation_users;
ALTER TABLE aips      RENAME COLUMN source_sip_id TO source_intake_id;
ALTER TABLE aips      RENAME TO preservations;

-- DIP -> Release
ALTER TABLE dip_users RENAME COLUMN dip_id TO release_id;
ALTER TABLE dip_users RENAME TO release_users;
ALTER TABLE dips      RENAME COLUMN source_aip_id TO source_preservation_id;
ALTER TABLE dips      RENAME TO releases;
