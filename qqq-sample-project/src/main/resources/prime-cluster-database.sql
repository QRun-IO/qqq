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

-- Shared process and async state (cluster mode only; see docs/DEPLOYMENT-MODES.md).
DROP TABLE IF EXISTS qqq_state;
CREATE TABLE qqq_state
(
   state_key VARCHAR(100) PRIMARY KEY,
   state_type VARCHAR(40),
   payload TEXT,
   node_id VARCHAR(250),
   create_date TIMESTAMP,
   modify_date TIMESTAMP,
   expires_at TIMESTAMP
);
CREATE INDEX i_qqq_state_expires_at ON qqq_state (expires_at);
