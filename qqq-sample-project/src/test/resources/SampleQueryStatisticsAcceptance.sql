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

-- Owned H2 statistics schema; explicit SQL keeps persistence assertions independent of metadata generation.
CREATE TABLE qqq_table (id INT AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP DEFAULT now(), modify_date TIMESTAMP DEFAULT now(), name VARCHAR(100) UNIQUE, label VARCHAR(100));
CREATE TABLE qqq_table_cache (id INT PRIMARY KEY, create_date TIMESTAMP DEFAULT now(), modify_date TIMESTAMP DEFAULT now(), name VARCHAR(100) UNIQUE, label VARCHAR(100));
CREATE TABLE query_stat (id INT AUTO_INCREMENT PRIMARY KEY, start_timestamp TIMESTAMP, first_result_timestamp TIMESTAMP,
 first_result_millis INT, qqq_table_id INT, action VARCHAR(100), session_id VARCHAR(36), query_text VARCHAR(65535));
CREATE TABLE query_stat_join_table (id INT AUTO_INCREMENT PRIMARY KEY, query_stat_id INT, qqq_table_id INT, type VARCHAR(50));
CREATE TABLE query_stat_criteria_field (id INT AUTO_INCREMENT PRIMARY KEY, query_stat_id INT, qqq_table_id INT,
 name VARCHAR(50), operator VARCHAR(30), criteria_values VARCHAR(50));
CREATE TABLE query_stat_order_by_field (id INT AUTO_INCREMENT PRIMARY KEY, query_stat_id INT, qqq_table_id INT, name VARCHAR(50));
