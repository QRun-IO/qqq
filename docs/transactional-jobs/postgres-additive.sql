-- The three nullable columns are required BEFORE deploying the new core metadata,
-- including hosts that keep strict admission disabled. Guard setup is opt-in.
-- Trusted additive migration after the complete baseline Jobs schema exists, run by a
-- schema-owner role (never by the DML-only application role). All participating Jobs
-- tables must be permanent (logged) ordinary tables with deterministic collations.
-- This file does not silently alter existing identity semantics.
ALTER TABLE jobs_job
  ADD COLUMN enlistment_key VARCHAR(64) NULL,
  ADD COLUMN intent_fingerprint VARCHAR(64) NULL,
  ADD COLUMN process_revision VARCHAR(128) NULL,
  ADD CONSTRAINT jobs_job_enlistment_unique UNIQUE(enlistment_key);
CREATE TABLE jobs_admission(id INTEGER PRIMARY KEY);
INSERT INTO jobs_admission(id) VALUES(1);
-- Grant the DML-only application role SELECT, INSERT, UPDATE and DELETE on every Jobs
-- table (and USAGE on its sequences); it must not own any of them. Configure durable
-- server settings (fsync, full_page_writes, synchronous_commit on or remote_apply), a
-- READ COMMITTED default, a UTC session and a 1-5000 ms lock_timeout at the role or
-- pool level, before enabling strict admission.
