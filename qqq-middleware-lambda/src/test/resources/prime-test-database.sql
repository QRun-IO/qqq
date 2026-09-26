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

DROP TABLE IF EXISTS person;
CREATE TABLE person
(
   id INT AUTO_INCREMENT,
   create_date TIMESTAMP DEFAULT now(),
   modify_date TIMESTAMP DEFAULT now(),

   first_name VARCHAR(80) NOT NULL,
   last_name VARCHAR(80) NOT NULL,
   birth_date DATE,
   email VARCHAR(250) NOT NULL
);

INSERT INTO person (id, first_name, last_name, birth_date, email) VALUES (1, 'Darin', 'Kelkhoff', '1980-05-31', 'darin.kelkhoff@gmail.com');
INSERT INTO person (id, first_name, last_name, birth_date, email) VALUES (2, 'James', 'Maes', '1980-05-15', 'jmaes@mmltholdings.com');
INSERT INTO person (id, first_name, last_name, birth_date, email) VALUES (3, 'Tim', 'Chamberlain', '1976-05-28', 'tchamberlain@mmltholdings.com');
INSERT INTO person (id, first_name, last_name, birth_date, email) VALUES (4, 'Tyler', 'Samples', '1990-01-01', 'tsamples@mmltholdings.com');
INSERT INTO person (id, first_name, last_name, birth_date, email) VALUES (5, 'Garret', 'Richardson', '1981-01-01', 'grichardson@mmltholdings.com');
