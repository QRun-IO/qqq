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

DROP TABLE IF EXISTS person;
CREATE TABLE person
(
   id INTEGER PRIMARY KEY,
   create_date TIMESTAMP, -- DEFAULT datetime('now'), -- can't get this to work!
   modify_date TIMESTAMP, -- DEFAULT datetime('now'),

   first_name VARCHAR(80) NOT NULL,
   last_name VARCHAR(80) NOT NULL,
   birth_date DATE,
   email VARCHAR(250) NOT NULL,
   is_employed BOOLEAN,
   annual_salary DECIMAL(12,2),
   days_worked INTEGER,
   home_town VARCHAR(80),
   start_time TIME
);

INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked, home_town) VALUES (1, 'Darin', 'Kelkhoff', '1980-05-31', 'darin.kelkhoff@gmail.com', 1, 25000, 27, 'Chester');
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked, home_town) VALUES (2, 'James', 'Maes', '1980-05-15', 'jmaes@mmltholdings.com', 1, 26000, 124, 'Chester');
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked, home_town) VALUES (3, 'Tim', 'Chamberlain', '1976-05-28', 'tchamberlain@mmltholdings.com', 0, null, 0, 'Decatur');
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked, home_town) VALUES (4, 'Tyler', 'Samples', NULL, 'tsamples@mmltholdings.com', 1, 30000, 99, 'Texas');
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked, home_town) VALUES (5, 'Garret', 'Richardson', '1981-01-01', 'grichardson@mmltholdings.com', 1, 1000000, 232, null);

DROP TABLE IF EXISTS personal_id_card;
CREATE TABLE personal_id_card
(
   id INTEGER PRIMARY KEY,
   create_date TIMESTAMP, -- DEFAULT date(),
   modify_date TIMESTAMP, -- DEFAULT date(),
   person_id INTEGER,
   id_number VARCHAR(250)
);

INSERT INTO personal_id_card (person_id, id_number) VALUES (1, '19800531');
INSERT INTO personal_id_card (person_id, id_number) VALUES (2, '19800515');
INSERT INTO personal_id_card (person_id, id_number) VALUES (3, '19760528');
INSERT INTO personal_id_card (person_id, id_number) VALUES (6, '123123123');
INSERT INTO personal_id_card (person_id, id_number) VALUES (null, '987987987');
INSERT INTO personal_id_card (person_id, id_number) VALUES (null, '456456456');

DROP TABLE IF EXISTS carrier;
CREATE TABLE carrier
(
   id INTEGER PRIMARY KEY,
   name VARCHAR(80) NOT NULL,
   company_code VARCHAR(80) NOT NULL,
   service_level VARCHAR(80) NOT NULL
);

INSERT INTO carrier (id, name, company_code, service_level) VALUES (1, 'UPS Ground', 'UPS', 'G');
INSERT INTO carrier (id, name, company_code, service_level) VALUES (2, 'UPS 2Day', 'UPS', '2');
INSERT INTO carrier (id, name, company_code, service_level) VALUES (3, 'UPS International', 'UPS', 'I');
INSERT INTO carrier (id, name, company_code, service_level) VALUES (4, 'Fedex Ground', 'FEDEX', 'G');
INSERT INTO carrier (id, name, company_code, service_level) VALUES (5, 'Fedex Next Day', 'UPS', '1');
INSERT INTO carrier (id, name, company_code, service_level) VALUES (6, 'Will Call', 'WILL_CALL', 'W');
INSERT INTO carrier (id, name, company_code, service_level) VALUES (7, 'USPS Priority', 'USPS', '1');
INSERT INTO carrier (id, name, company_code, service_level) VALUES (8, 'USPS Super Slow', 'USPS', '4');
INSERT INTO carrier (id, name, company_code, service_level) VALUES (9, 'USPS Super Fast', 'USPS', '0');
INSERT INTO carrier (id, name, company_code, service_level) VALUES (10, 'DHL International', 'DHL', 'I');
INSERT INTO carrier (id, name, company_code, service_level) VALUES (11, 'GSO', 'GSO', 'G');

DROP TABLE IF EXISTS line_item_extrinsic;
DROP TABLE IF EXISTS order_line;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS order_instructions;
DROP TABLE IF EXISTS warehouse_store_int;
DROP TABLE IF EXISTS store;
DROP TABLE IF EXISTS warehouse;

CREATE TABLE store
(
   id INTEGER PRIMARY KEY,
   name VARCHAR(80) NOT NULL
);

-- define 3 stores
INSERT INTO store (id, name) VALUES (1, 'Q-Mart');
INSERT INTO store (id, name) VALUES (2, 'QQQ ''R'' Us');
INSERT INTO store (id, name) VALUES (3, 'QDepot');

CREATE TABLE item
(
   id INTEGER PRIMARY KEY,
   sku VARCHAR(80) NOT NULL,
   description VARCHAR(80),
   store_id INT NOT NULL REFERENCES store
);

-- three items for each store
INSERT INTO item (id, sku, description, store_id) VALUES (1, 'QM-1', 'Q-Mart Item 1', 1);
INSERT INTO item (id, sku, description, store_id) VALUES (2, 'QM-2', 'Q-Mart Item 2', 1);
INSERT INTO item (id, sku, description, store_id) VALUES (3, 'QM-3', 'Q-Mart Item 3', 1);
INSERT INTO item (id, sku, description, store_id) VALUES (4, 'QRU-1', 'QQQ R Us Item 4', 2);
INSERT INTO item (id, sku, description, store_id) VALUES (5, 'QRU-2', 'QQQ R Us Item 5', 2);
INSERT INTO item (id, sku, description, store_id) VALUES (6, 'QRU-3', 'QQQ R Us Item 6', 2);
INSERT INTO item (id, sku, description, store_id) VALUES (7, 'QD-1', 'QDepot Item 7', 3);
INSERT INTO item (id, sku, description, store_id) VALUES (8, 'QD-2', 'QDepot Item 8', 3);
INSERT INTO item (id, sku, description, store_id) VALUES (9, 'QD-3', 'QDepot Item 9', 3);

CREATE TABLE `order`
(
   id INTEGER PRIMARY KEY,
   store_id INT REFERENCES store,
   bill_to_person_id INT,
   ship_to_person_id INT,
   current_order_instructions_id INT -- f-key to order_instructions, which also has an f-key back here!
);

-- variable orders
INSERT INTO `order` (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (1, 1, 1, 1);
INSERT INTO `order` (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (2, 1, 1, 2);
INSERT INTO `order` (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (3, 1, 2, 3);
INSERT INTO `order` (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (4, 2, 4, 5);
INSERT INTO `order` (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (5, 2, 5, 4);
INSERT INTO `order` (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (6, 3, 5, null);
INSERT INTO `order` (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (7, 3, null, 5);
INSERT INTO `order` (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (8, 3, null, 5);

CREATE TABLE order_instructions
(
   id INTEGER PRIMARY KEY,
   order_id INT,
   instructions VARCHAR(250)
);

-- give orders 1 & 2 multiple versions of the instruction record
INSERT INTO order_instructions (id, order_id, instructions) VALUES (1, 1, 'order 1 v1');
INSERT INTO order_instructions (id, order_id, instructions) VALUES (2, 1, 'order 1 v2');
UPDATE `order` SET current_order_instructions_id = 2 WHERE id=1;

INSERT INTO order_instructions (id, order_id, instructions) VALUES (3, 2, 'order 2 v1');
INSERT INTO order_instructions (id, order_id, instructions) VALUES (4, 2, 'order 2 v2');
INSERT INTO order_instructions (id, order_id, instructions) VALUES (5, 2, 'order 2 v3');
UPDATE `order` SET current_order_instructions_id = 5 WHERE id=2;

-- give all other orders just 1 instruction
INSERT INTO order_instructions (order_id, instructions) SELECT id, concat('order ', id, ' v1') FROM `order` WHERE current_order_instructions_id IS NULL;
UPDATE `order` SET current_order_instructions_id = (SELECT MIN(id) FROM order_instructions WHERE order_id = `order`.id) WHERE current_order_instructions_id is null;

CREATE TABLE order_line
(
   id INTEGER PRIMARY KEY,
   order_id INT REFERENCES `order`,
   sku VARCHAR(80),
   store_id INT REFERENCES store,
   quantity INT
);

-- various lines
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (1, 'QM-1', 1, 10);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (1, 'QM-2', 1, 1);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (1, 'QM-3', 1, 1);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (2, 'QRU-1', 2, 1); -- this line has an item from a different store than its order.
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (3, 'QM-1', 1, 20);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (4, 'QRU-1', 2, 1);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (4, 'QRU-2', 2, 2);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (5, 'QRU-1', 2, 1);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (6, 'QD-1', 3, 1);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (7, 'QD-1', 3, 2);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (8, 'QD-1', 3, 3);


CREATE TABLE warehouse
(
   id INTEGER PRIMARY KEY,
   name VARCHAR(80)
);

INSERT INTO warehouse (name) VALUES ('Patterson');
INSERT INTO warehouse (name) VALUES ('Edison');
INSERT INTO warehouse (name) VALUES ('Stockton');
INSERT INTO warehouse (name) VALUES ('Somewhere in Texas');

CREATE TABLE warehouse_store_int
(
   id INTEGER PRIMARY KEY,
   warehouse_id INT REFERENCES `warehouse`,
   store_id INT REFERENCES `store`
);

INSERT INTO warehouse_store_int (warehouse_id, store_id) VALUES (1, 1);
INSERT INTO warehouse_store_int (warehouse_id, store_id) VALUES (1, 2);
INSERT INTO warehouse_store_int (warehouse_id, store_id) VALUES (1, 3);

CREATE TABLE line_item_extrinsic
(
   id INTEGER PRIMARY KEY,
   order_line_id INT REFERENCES order_line,
   `key` VARCHAR(80),
   `value` VARCHAR(80)
);

