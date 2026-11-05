-- The three nullable columns are required BEFORE deploying the new core metadata,
-- including hosts that keep strict admission disabled. Guard setup is opt-in.
-- Trusted additive migration after the complete baseline Jobs schema exists.
-- All participating Jobs tables/character columns must use utf8mb4_0900_bin
-- and InnoDB. This file does not silently alter existing identity semantics.
ALTER TABLE jobs_job
  ADD COLUMN enlistment_key VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
  ADD COLUMN intent_fingerprint VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
  ADD COLUMN process_revision VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
  ADD CONSTRAINT jobs_job_enlistment_unique UNIQUE(enlistment_key);
CREATE TABLE jobs_admission(id INT PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO jobs_admission(id) VALUES(1);
-- Configure durable server settings and READ COMMITTED/UTC/bounded locks in
-- the trusted host pool configuration, before enabling strict admission.
