--
-- QQQ - Low-code Application Framework for Engineers.
-- Copyright (C) 2021-2022.  Kingsrook, LLC
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

-- Disposable PostgreSQL schema for the starter acceptance run.
-- QQQ's PostgreSQL strategy double-quotes every table and column name, so the
-- mixed-case names the starter and generated QBit use are quoted here too.
CREATE TABLE sample (
   id SERIAL PRIMARY KEY,
   name VARCHAR(255)
);
INSERT INTO sample (name) VALUES ('First example');

CREATE TABLE "orderDeskEntity" (
   id SERIAL PRIMARY KEY,
   name VARCHAR(100) NOT NULL,
   description VARCHAR(500),
   status VARCHAR(50),
   "isActive" BOOLEAN,
   "createDate" TIMESTAMP(3),
   "modifyDate" TIMESTAMP(3)
);

CREATE TABLE "orderDeskChildEntity" (
   id SERIAL PRIMARY KEY,
   "orderDeskEntityId" INT NOT NULL,
   name VARCHAR(100) NOT NULL,
   description VARCHAR(500),
   "sortOrder" INT,
   "isActive" BOOLEAN,
   "createDate" TIMESTAMP(3),
   "modifyDate" TIMESTAMP(3),
   CONSTRAINT fk_orderdesk_parent FOREIGN KEY ("orderDeskEntityId") REFERENCES "orderDeskEntity"(id)
);
