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

-- Drop all tables and sequences in correct order (reverse dependency)
DROP SEQUENCE IF EXISTS line_item_extrinsic_id_seq CASCADE;
DROP SEQUENCE IF EXISTS order_line_id_seq CASCADE;
DROP SEQUENCE IF EXISTS order_table_id_seq CASCADE;
DROP SEQUENCE IF EXISTS order_instructions_id_seq CASCADE;
DROP SEQUENCE IF EXISTS item_id_seq CASCADE;
DROP SEQUENCE IF EXISTS warehouse_store_int_id_seq CASCADE;
DROP SEQUENCE IF EXISTS warehouse_id_seq CASCADE;
DROP SEQUENCE IF EXISTS store_id_seq CASCADE;
DROP SEQUENCE IF EXISTS personal_id_card_id_seq CASCADE;
DROP SEQUENCE IF EXISTS carrier_id_seq CASCADE;
DROP SEQUENCE IF EXISTS person_id_seq CASCADE;

DROP TABLE IF EXISTS line_item_extrinsic CASCADE;
DROP TABLE IF EXISTS order_line CASCADE;
DROP TABLE IF EXISTS order_table CASCADE;
DROP TABLE IF EXISTS order_instructions CASCADE;
DROP TABLE IF EXISTS item CASCADE;
DROP TABLE IF EXISTS warehouse_store_int CASCADE;
DROP TABLE IF EXISTS warehouse CASCADE;
DROP TABLE IF EXISTS store CASCADE;
DROP TABLE IF EXISTS personal_id_card CASCADE;
DROP TABLE IF EXISTS carrier CASCADE;
DROP TABLE IF EXISTS person CASCADE;

-- Create person table
CREATE TABLE person
(
   id SERIAL PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
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

INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked, home_town) 
VALUES (1, 'Darin', 'Kelkhoff', '1980-05-31', 'darin.kelkhoff@gmail.com', true, 25000, 27, 'Chester');
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked, home_town) 
VALUES (2, 'James', 'Maes', '1980-05-15', 'jmaes@mmltholdings.com', true, 26000, 124, 'Chester');
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked, home_town) 
VALUES (3, 'Tim', 'Chamberlain', '1976-05-28', 'tchamberlain@mmltholdings.com', false, null, 0, 'Decatur');
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked, home_town) 
VALUES (4, 'Tyler', 'Samples', NULL, 'tsamples@mmltholdings.com', true, 30000, 99, 'Texas');
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked, home_town) 
VALUES (5, 'Garret', 'Richardson', '1981-01-01', 'grichardson@mmltholdings.com', true, 1000000, 232, null);
ALTER SEQUENCE person_id_seq RESTART WITH 6;

CREATE TABLE personal_id_card
(
   id SERIAL PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   person_id INTEGER,
   id_number VARCHAR(250)
);

INSERT INTO personal_id_card (person_id, id_number) VALUES (1, '19800531');
INSERT INTO personal_id_card (person_id, id_number) VALUES (2, '19800515');
INSERT INTO personal_id_card (person_id, id_number) VALUES (3, '19760528');
INSERT INTO personal_id_card (person_id, id_number) VALUES (6, '123123123');
INSERT INTO personal_id_card (person_id, id_number) VALUES (null, '987987987');
INSERT INTO personal_id_card (person_id, id_number) VALUES (null, '456456456');
ALTER SEQUENCE personal_id_card_id_seq RESTART WITH 7;

CREATE TABLE carrier
(
   id SERIAL PRIMARY KEY,
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
ALTER SEQUENCE carrier_id_seq RESTART WITH 12;

CREATE TABLE store
(
   id SERIAL PRIMARY KEY,
   name VARCHAR(80) NOT NULL
);

-- define 3 stores
INSERT INTO store (id, name) VALUES (1, 'Q-Mart');
INSERT INTO store (id, name) VALUES (2, 'QQQ ''R'' Us');
INSERT INTO store (id, name) VALUES (3, 'QDepot');
ALTER SEQUENCE store_id_seq RESTART WITH 4;

CREATE TABLE item
(
   id SERIAL PRIMARY KEY,
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
ALTER SEQUENCE item_id_seq RESTART WITH 10;

CREATE TABLE order_table
(
   id SERIAL PRIMARY KEY,
   store_id INT REFERENCES store,
   bill_to_person_id INT,
   ship_to_person_id INT,
   current_order_instructions_id INT
);

-- variable orders
INSERT INTO order_table (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (1, 1, 1, 1);
INSERT INTO order_table (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (2, 1, 1, 2);
INSERT INTO order_table (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (3, 1, 2, 3);
INSERT INTO order_table (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (4, 2, 4, 5);
INSERT INTO order_table (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (5, 2, 5, 4);
INSERT INTO order_table (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (6, 3, 5, null);
INSERT INTO order_table (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (7, 3, null, 5);
INSERT INTO order_table (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (8, 3, null, 5);
ALTER SEQUENCE order_table_id_seq RESTART WITH 9;

CREATE TABLE order_instructions
(
   id SERIAL PRIMARY KEY,
   order_id INT,
   instructions VARCHAR(250)
);

-- give orders 1 & 2 multiple versions of the instruction record
INSERT INTO order_instructions (id, order_id, instructions) VALUES (1, 1, 'order 1 v1');
INSERT INTO order_instructions (id, order_id, instructions) VALUES (2, 1, 'order 1 v2');
UPDATE order_table SET current_order_instructions_id = 2 WHERE id=1;

INSERT INTO order_instructions (id, order_id, instructions) VALUES (3, 2, 'order 2 v1');
INSERT INTO order_instructions (id, order_id, instructions) VALUES (4, 2, 'order 2 v2');
INSERT INTO order_instructions (id, order_id, instructions) VALUES (5, 2, 'order 2 v3');
UPDATE order_table SET current_order_instructions_id = 5 WHERE id=2;

-- Set sequence before dynamic inserts
ALTER SEQUENCE order_instructions_id_seq RESTART WITH 6;

-- give all other orders just 1 instruction
INSERT INTO order_instructions (order_id, instructions) 
SELECT id, 'order ' || id || ' v1' FROM order_table WHERE current_order_instructions_id IS NULL;
UPDATE order_table SET current_order_instructions_id = (SELECT MIN(id) FROM order_instructions WHERE order_id = order_table.id) 
WHERE current_order_instructions_id is null;

CREATE TABLE order_line
(
   id SERIAL PRIMARY KEY,
   order_id INT REFERENCES order_table,
   sku VARCHAR(80),
   store_id INT REFERENCES store,
   quantity INT
);

-- various lines
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (1, 'QM-1', 1, 10);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (1, 'QM-2', 1, 1);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (1, 'QM-3', 1, 1);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (2, 'QRU-1', 2, 1);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (3, 'QM-1', 1, 20);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (4, 'QRU-1', 2, 1);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (4, 'QRU-2', 2, 2);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (5, 'QRU-1', 2, 1);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (6, 'QD-1', 3, 1);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (7, 'QD-1', 3, 2);
INSERT INTO order_line (order_id, sku, store_id, quantity) VALUES (8, 'QD-1', 3, 3);
ALTER SEQUENCE order_line_id_seq RESTART WITH 20;

CREATE TABLE warehouse
(
   id SERIAL PRIMARY KEY,
   name VARCHAR(80)
);

INSERT INTO warehouse (name) VALUES ('Patterson');
INSERT INTO warehouse (name) VALUES ('Edison');
INSERT INTO warehouse (name) VALUES ('Stockton');
INSERT INTO warehouse (name) VALUES ('Somewhere in Texas');
ALTER SEQUENCE warehouse_id_seq RESTART WITH 5;

CREATE TABLE warehouse_store_int
(
   id SERIAL PRIMARY KEY,
   warehouse_id INT REFERENCES warehouse,
   store_id INT REFERENCES store
);

INSERT INTO warehouse_store_int (warehouse_id, store_id) VALUES (1, 1);
INSERT INTO warehouse_store_int (warehouse_id, store_id) VALUES (1, 2);
INSERT INTO warehouse_store_int (warehouse_id, store_id) VALUES (1, 3);
ALTER SEQUENCE warehouse_store_int_id_seq RESTART WITH 4;

CREATE TABLE line_item_extrinsic
(
   id SERIAL PRIMARY KEY,
   order_line_id INT REFERENCES order_line,
   key VARCHAR(80),
   value VARCHAR(80)
);
ALTER SEQUENCE line_item_extrinsic_id_seq RESTART WITH 1;
