-- The three nullable columns are required BEFORE deploying the new core metadata,
-- including hosts that keep strict admission disabled. Guard setup is opt-in.
-- Trusted additive migration after the complete baseline Jobs schema exists.
-- Execute before enabling transactionalAdmissionEnabled; never runtime DDL.
ALTER TABLE jobs_job ADD COLUMN enlistment_key VARCHAR(64);
ALTER TABLE jobs_job ADD COLUMN intent_fingerprint VARCHAR(64);
ALTER TABLE jobs_job ADD COLUMN process_revision VARCHAR(128);
ALTER TABLE jobs_job ADD CONSTRAINT jobs_job_enlistment_unique UNIQUE(enlistment_key);
CREATE TABLE jobs_admission(id INTEGER PRIMARY KEY);
INSERT INTO jobs_admission(id) VALUES(1);
-- Host connection: file H2, DATABASE_TO_UPPER=FALSE, WRITE_DELAY=0,
-- READ COMMITTED, and LOCK_TIMEOUT between 1 and 5000 milliseconds.
