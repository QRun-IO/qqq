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

ALTER TABLE person ADD associated_script_id INTEGER;
CREATE TABLE script_type
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP,
   modify_date TIMESTAMP,
   name VARCHAR(100),
   help_text CLOB,
   sample_code CLOB,
   file_mode INTEGER,
   test_script_interface_name VARCHAR(500)
);

CREATE TABLE script_type_file_schema
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP,
   modify_date TIMESTAMP,
   script_type_id INTEGER,
   name VARCHAR(100),
   file_type VARCHAR(50)
);

CREATE TABLE script
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP,
   modify_date TIMESTAMP,
   name VARCHAR(250),
   script_type_id INTEGER,
   table_name VARCHAR(100),
   max_batch_size INTEGER,
   current_script_revision_id INTEGER
);

CREATE TABLE script_revision
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP,
   modify_date TIMESTAMP,
   script_id INTEGER,
   api_name VARCHAR(100),
   api_version VARCHAR(100),
   sequence_no INTEGER,
   commit_message VARCHAR(250),
   author VARCHAR(100)
);

CREATE TABLE script_revision_file
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP,
   modify_date TIMESTAMP,
   script_revision_id INTEGER,
   file_name VARCHAR(100),
   contents CLOB
);

CREATE TABLE script_log
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP,
   modify_date TIMESTAMP,
   script_id INTEGER,
   script_revision_id INTEGER,
   start_timestamp TIMESTAMP,
   end_timestamp TIMESTAMP,
   run_time_millis INTEGER,
   had_error BOOLEAN,
   input CLOB,
   output CLOB,
   error CLOB
);

CREATE TABLE script_log_line
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP,
   modify_date TIMESTAMP,
   script_log_id INTEGER,
   timestamp TIMESTAMP,
   text CLOB
);

CREATE TABLE table_trigger
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP,
   modify_date TIMESTAMP,
   table_name VARCHAR(100),
   filter_id INTEGER,
   script_id INTEGER,
   priority INTEGER,
   post_insert BOOLEAN,
   post_update BOOLEAN
);
