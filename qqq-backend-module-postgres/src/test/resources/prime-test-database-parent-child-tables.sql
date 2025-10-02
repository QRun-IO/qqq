--
-- QQQ - Low-code Application Framework for Engineers.
-- Copyright (C) 2021-2025.  Kingsrook, LLC
-- 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
-- contact@kingsrook.com
-- https://github.com/Kingsrook/
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as
-- published by the Free Software Foundation, either version 3 of the
-- License, or (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program.  If not, see <https://www.gnu.org/licenses/>.
--

DROP TABLE IF EXISTS parent_table CASCADE;
DROP TABLE IF EXISTS child_table CASCADE;

CREATE TABLE child_table
(
   id SERIAL PRIMARY KEY,
   name VARCHAR(80) NOT NULL
);

INSERT INTO child_table (id, name) VALUES (1, 'Timmy');
INSERT INTO child_table (id, name) VALUES (2, 'Jimmy');
INSERT INTO child_table (id, name) VALUES (3, 'Johnny');
INSERT INTO child_table (id, name) VALUES (4, 'Gracie');
INSERT INTO child_table (id, name) VALUES (5, 'Suzie');

CREATE TABLE parent_table
(
   id SERIAL PRIMARY KEY,
   name VARCHAR(80) NOT NULL,
   child_id INT,
   FOREIGN KEY (child_id) REFERENCES child_table(id)
);

INSERT INTO parent_table (id, name, child_id) VALUES (1, 'Tim''s Dad', 1);
INSERT INTO parent_table (id, name, child_id) VALUES (2, 'Tim''s Mom', 1);
INSERT INTO parent_table (id, name, child_id) VALUES (3, 'Childless Man', null);
INSERT INTO parent_table (id, name, child_id) VALUES (4, 'Childless Woman', null);
INSERT INTO parent_table (id, name, child_id) VALUES (5, 'Johny''s Single Dad', 3);
