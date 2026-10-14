--
-- QQQ - Low-code Application Framework for Engineers.
-- Copyright (C) 2021-2026.  Kingsrook, LLC
-- 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
-- contact@kingsrook.com
-- https://github.com/Kingsrook/
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- Durable jobs tables (snake_case columns; use DurableJobsMetaDataProvider.snakeCaseColumnNames()),
-- plus the tables the kill-the-pod tests load into.
DROP TABLE IF EXISTS jobs_row_error CASCADE;
DROP TABLE IF EXISTS jobs_event CASCADE;
DROP TABLE IF EXISTS jobs_chunk CASCADE;
DROP TABLE IF EXISTS jobs_worker CASCADE;
DROP TABLE IF EXISTS jobs_job CASCADE;
DROP TABLE IF EXISTS kill_test_step_log CASCADE;
DROP TABLE IF EXISTS kill_test_target CASCADE;
DROP TABLE IF EXISTS jobs_storage CASCADE;

CREATE TABLE jobs_job
(
   id                        SERIAL PRIMARY KEY,
   label                     VARCHAR(250),
   process_name              VARCHAR(250) NOT NULL,
   table_name                VARCHAR(250),
   mode                      VARCHAR(20),
   status                    VARCHAR(30) NOT NULL,
   status_reason             VARCHAR(500),
   priority                  INTEGER DEFAULT 5,
   user_id                   VARCHAR(250),
   client_channel            VARCHAR(250),
   credential_scope_json     TEXT,
   source_type               VARCHAR(30),
   source_json               TEXT,
   params_json               TEXT,
   idempotency               VARCHAR(30),
   semantics                 VARCHAR(20),
   chunk_size                INTEGER,
   max_parallel_chunks       INTEGER,
   rows_total                INTEGER,
   rows_done                 INTEGER,
   rows_ok                   INTEGER,
   rows_failed               INTEGER,
   chunks_total              INTEGER,
   chunks_done               INTEGER,
   chunks_failed             INTEGER,
   summary_json              TEXT,
   output_storage_json       TEXT,
   parent_job_id             INTEGER,
   saved_bulk_load_profile_id INTEGER,
   notify_email              BOOLEAN DEFAULT FALSE,
   queued_at                 TIMESTAMPTZ,
   started_at                TIMESTAMPTZ,
   finished_at               TIMESTAMPTZ,
   last_progress_at          TIMESTAMPTZ,
   error_message             TEXT,
   lease_version             INTEGER DEFAULT 0,
   create_date               TIMESTAMPTZ,
   modify_date               TIMESTAMPTZ
);
CREATE INDEX jobs_job_claim_idx ON jobs_job (status, priority, queued_at);
CREATE INDEX jobs_job_user_idx ON jobs_job (user_id, status);

CREATE TABLE jobs_chunk
(
   id                SERIAL PRIMARY KEY,
   job_id            INTEGER NOT NULL REFERENCES jobs_job (id) ON DELETE CASCADE,
   seq               INTEGER NOT NULL,
   status            VARCHAR(30) NOT NULL,
   range_json        TEXT,
   input_storage_ref VARCHAR(1000),
   attempt           INTEGER DEFAULT 0,
   max_attempts      INTEGER DEFAULT 3,
   lease_owner       VARCHAR(250),
   lease_token       VARCHAR(100),
   lease_expires_at  TIMESTAMPTZ,
   heartbeat_at      TIMESTAMPTZ,
   rows_in           INTEGER,
   rows_ok           INTEGER,
   rows_failed       INTEGER,
   summary_json      TEXT,
   checkpoint_json   TEXT,
   error_message     TEXT,
   started_at        TIMESTAMPTZ,
   finished_at       TIMESTAMPTZ,
   lease_version     INTEGER DEFAULT 0,
   create_date       TIMESTAMPTZ,
   modify_date       TIMESTAMPTZ,
   UNIQUE (job_id, seq)
);
CREATE INDEX jobs_chunk_status_idx ON jobs_chunk (job_id, status);
CREATE INDEX jobs_chunk_lease_idx ON jobs_chunk (status, lease_expires_at);

CREATE TABLE jobs_row_error
(
   id           SERIAL PRIMARY KEY,
   job_id       INTEGER NOT NULL REFERENCES jobs_job (id) ON DELETE CASCADE,
   chunk_id     INTEGER,
   row_number   INTEGER,
   record_pk    VARCHAR(250),
   field_name   VARCHAR(250),
   error_code   VARCHAR(100),
   message      TEXT,
   retry_job_id INTEGER,
   resolved     BOOLEAN DEFAULT FALSE,
   create_date  TIMESTAMPTZ
);
CREATE INDEX jobs_row_error_job_idx ON jobs_row_error (job_id);

CREATE TABLE jobs_event
(
   id             SERIAL PRIMARY KEY,
   job_id         INTEGER NOT NULL REFERENCES jobs_job (id) ON DELETE CASCADE,
   chunk_id       INTEGER,
   type           VARCHAR(30),
   actor_user_id  VARCHAR(250),
   client_channel VARCHAR(250),
   pod            VARCHAR(250),
   detail         TEXT,
   create_date    TIMESTAMPTZ
);
CREATE INDEX jobs_event_job_idx ON jobs_event (job_id);

CREATE TABLE jobs_worker
(
   id           SERIAL PRIMARY KEY,
   pod_id       VARCHAR(250) NOT NULL UNIQUE,
   started_at   TIMESTAMPTZ,
   heartbeat_at TIMESTAMPTZ,
   threads      INTEGER,
   draining     BOOLEAN,
   version      VARCHAR(100)
);

CREATE TABLE kill_test_step_log
(
   id          SERIAL PRIMARY KEY,
   step_name   VARCHAR(50),
   pod_id      VARCHAR(250),
   create_date TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE kill_test_target
(
   id          SERIAL PRIMARY KEY,
   code        VARCHAR(50) NOT NULL UNIQUE,
   name        VARCHAR(250) NOT NULL,
   amount      INTEGER,
   jobs_dedupe_key VARCHAR(100) UNIQUE
);

CREATE TABLE jobs_storage
(
   reference VARCHAR(1000) PRIMARY KEY,
   contents  BYTEA
);
