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

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
   id          INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   username    VARCHAR(100)
);


DROP TABLE IF EXISTS `group`;
CREATE TABLE `group`
(
   id          INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   name        VARCHAR(100),
   client_id   INTEGER
);


DROP TABLE IF EXISTS `client`;
CREATE TABLE `client`
(
   id          INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   name        VARCHAR(100)
);


DROP TABLE IF EXISTS asset;
CREATE TABLE asset
(
   id          INT AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   name        VARCHAR(100),
   user_id     INTEGER
);


DROP TABLE IF EXISTS shared_asset;
CREATE TABLE shared_asset
(
   id          INT AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   asset_id    INTEGER,
   user_id     INTEGER,
   group_id    INTEGER
);

