--
-- QQQ - Low-code Application Framework for Engineers.
-- Copyright (C) 2021-2025.  Kingsrook, LLC
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

DROP TABLE IF EXISTS parent_table;
DROP TABLE IF EXISTS child_table;

CREATE TABLE child_table
(
   id INT AUTO_INCREMENT primary key,
   name VARCHAR(80) NOT NULL
);

INSERT INTO child_table (id, name) VALUES (1, 'Timmy');
INSERT INTO child_table (id, name) VALUES (2, 'Jimmy');
INSERT INTO child_table (id, name) VALUES (3, 'Johnny');
INSERT INTO child_table (id, name) VALUES (4, 'Gracie');
INSERT INTO child_table (id, name) VALUES (5, 'Suzie');

CREATE TABLE parent_table
(
   id INT AUTO_INCREMENT PRIMARY KEY,
   name VARCHAR(80) NOT NULL,
   child_id INT,
   foreign key (child_id) references child_table(id)
);

INSERT INTO parent_table (id, name, child_id) VALUES (1, 'Tim''s Dad', 1);
INSERT INTO parent_table (id, name, child_id) VALUES (2, 'Tim''s Mom', 1);
INSERT INTO parent_table (id, name, child_id) VALUES (3, 'Childless Man', null);
INSERT INTO parent_table (id, name, child_id) VALUES (4, 'Childless Woman', null);
INSERT INTO parent_table (id, name, child_id) VALUES (5, 'Johny''s Single Dad', 3);
