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

DROP TABLE IF EXISTS user_session;
CREATE TABLE user_session
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT now(),
   modify_date TIMESTAMP DEFAULT now(),
   uuid VARCHAR(40) NOT NULL,
   access_token MEDIUMTEXT,
   user_id VARCHAR(100)
);
ALTER TABLE user_session ADD UNIQUE u_uuid (uuid);
ALTER TABLE user_session ADD INDEX i_user_id (user_id);


DROP TABLE IF EXISTS redirect_state;
CREATE TABLE redirect_state
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT now(),
   state VARCHAR(45) NOT NULL,
   redirect_uri TEXT
);
ALTER TABLE redirect_state ADD UNIQUE u_state (state);


DROP TABLE IF EXISTS person;
CREATE TABLE person
(
   id INT AUTO_INCREMENT primary key ,
   create_date TIMESTAMP DEFAULT now(),
   modify_date TIMESTAMP DEFAULT now(),

   first_name VARCHAR(80) NOT NULL,
   last_name VARCHAR(80) NOT NULL,
   birth_date DATE,
   email VARCHAR(250) NOT NULL,

   is_employed BOOLEAN,
   annual_salary DECIMAL(12, 2),
   days_worked INTEGER
);

INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked) VALUES (1, 'Avery', 'Sample', '1990-01-15', 'avery@example.invalid', 1, 75003.50, 1001);
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked) VALUES (2, 'Blair', 'Sample', '1991-02-16', 'blair@example.invalid', 1, 150000, 10100);
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked) VALUES (3, 'Casey', 'Sample', '1992-03-17', 'casey@example.invalid', 1, 300000, 100100);
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked) VALUES (4, 'Drew', 'Sample', NULL, 'drew@example.invalid', 1, 950000, 75);
INSERT INTO person (id, first_name, last_name, birth_date, email, is_employed, annual_salary, days_worked) VALUES (5, 'Morgan', 'Sample', '1993-04-18', 'morgan@example.invalid', 0, 1500000, 1);

DROP TABLE IF EXISTS pet;
CREATE TABLE pet
(
   id INT AUTO_INCREMENT primary key ,
   create_date TIMESTAMP DEFAULT now(),
   modify_date TIMESTAMP DEFAULT now(),

   name VARCHAR(80) NOT NULL,
   species_id INTEGER NOT NULL,
   person_id INTEGER NOT NULL,
   birth_date DATE
);

INSERT INTO pet (id, name, species_id, person_id) VALUES (1, 'Charlie', 1, 1);
INSERT INTO pet (id, name, species_id, person_id) VALUES (2, 'Coco', 1, 1);
INSERT INTO pet (id, name, species_id, person_id) VALUES (3, 'Louie', 1, 1);
INSERT INTO pet (id, name, species_id, person_id) VALUES (4, 'Barkley', 1, 1);
INSERT INTO pet (id, name, species_id, person_id) VALUES (5, 'Toby', 1, 2);
INSERT INTO pet (id, name, species_id, person_id) VALUES (6, 'Mae', 2, 3);


DROP TABLE IF EXISTS pet_note;
CREATE TABLE pet_note
(
   id INT AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT now(),
   modify_date TIMESTAMP DEFAULT now(),
   pet_id INTEGER NOT NULL,
   note VARCHAR(80) NOT NULL
);

INSERT INTO pet_note (id, pet_id, note) VALUES (1, 1, 'Target note');
INSERT INTO pet_note (id, pet_id, note) VALUES (2, 5, 'Other parent note');


DROP TABLE IF EXISTS carrier;
CREATE TABLE carrier
(
   id INT AUTO_INCREMENT PRIMARY KEY,
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

DROP TABLE IF EXISTS field_lab;
CREATE TABLE field_lab
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   name VARCHAR(80) NOT NULL UNIQUE,
   long_value BIGINT,
   decimal_value DECIMAL(20, 4),
   boolean_value BOOLEAN,
   date_value DATE,
   time_value TIME,
   date_time_value TIMESTAMP,
   fixed_zone_date_time TIMESTAMP,
   record_zone_date_time TIMESTAMP,
   time_zone VARCHAR(80),
   user_id_value VARCHAR(255),
   create_date TIMESTAMP,
   modify_date TIMESTAMP,
   created_day DATE,
   modified_day DATE,
   manual_date_time TIMESTAMP,
   text_value TEXT,
   html_value TEXT,
   password_value VARCHAR(255),
   blob_value BLOB,
   truncate_value VARCHAR(255),
   ellipsis_value VARCHAR(255),
   reject_long_value VARCHAR(255),
   pass_through_value VARCHAR(255),
   upper_value VARCHAR(255),
   lower_value VARCHAR(255),
   unchanged_value VARCHAR(255),
   trim_value VARCHAR(255),
   trim_left_value VARCHAR(255),
   trim_right_value VARCHAR(255),
   remove_space_value VARCHAR(255),
   normalized_key VARCHAR(255) UNIQUE,
   bounded_value DECIMAL(10, 2),
   exclusive_bounded_value DECIMAL(10, 2),
   inclusive_clipped_value DECIMAL(10, 2),
   clipped_value DECIMAL(10, 2)
);


-- Related-record picker sample (docs/RELATED-RECORD-PICKER.md): customers, SKUs, tags, sales orders
DROP TABLE IF EXISTS sales_order_tag;
DROP TABLE IF EXISTS sales_order_line;
DROP TABLE IF EXISTS sales_order;
DROP TABLE IF EXISTS tag;
DROP TABLE IF EXISTS sku;
DROP TABLE IF EXISTS customer;
CREATE TABLE customer
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   name VARCHAR(100) NOT NULL,
   email VARCHAR(250),
   phone VARCHAR(40),
   region VARCHAR(40),
   is_active BOOLEAN DEFAULT TRUE
);
CREATE TABLE sku
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   code VARCHAR(40) NOT NULL,
   name VARCHAR(100) NOT NULL,
   description VARCHAR(250),
   price DECIMAL(12, 2),
   region VARCHAR(40),
   is_active BOOLEAN DEFAULT TRUE
);
CREATE TABLE tag
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   name VARCHAR(40) NOT NULL
);
CREATE TABLE sales_order
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   order_no VARCHAR(40) NOT NULL,
   customer_id INTEGER,
   region VARCHAR(40),
   order_date DATE
);
CREATE TABLE sales_order_line
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   sales_order_id INTEGER NOT NULL,
   sku_id INTEGER NOT NULL,
   quantity INTEGER NOT NULL,
   unit_price DECIMAL(12, 2)
);
CREATE TABLE sales_order_tag
(
   id INTEGER AUTO_INCREMENT PRIMARY KEY,
   create_date TIMESTAMP DEFAULT NOW(),
   modify_date TIMESTAMP DEFAULT NOW(),
   sales_order_id INTEGER NOT NULL,
   tag_id INTEGER NOT NULL
);

INSERT INTO customer (id, name, email, phone, region, is_active) VALUES (1, 'Acme Corp', 'orders@acme.example', '555-0100', 'West', TRUE);
INSERT INTO customer (id, name, email, phone, region, is_active) VALUES (2, 'Bolt Brothers', 'buy@boltbros.example', '555-0101', 'East', TRUE);
INSERT INTO customer (id, name, email, phone, region, is_active) VALUES (3, 'Cog & Co', 'hello@cogco.example', '555-0102', 'West', TRUE);
INSERT INTO customer (id, name, email, phone, region, is_active) VALUES (4, 'Delta Supply', 'purchasing@delta.example', '555-0103', 'East', TRUE);
INSERT INTO customer (id, name, email, phone, region, is_active) VALUES (5, 'Echo Industries', 'ap@echo.example', '555-0104', 'West', TRUE);
INSERT INTO customer (id, name, email, phone, region, is_active) VALUES (6, 'Foxtrot Retail', 'store@foxtrot.example', '555-0105', 'East', FALSE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (1, 'WID-100', 'Widget', 'Standard widget', 9.50, 'West', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (2, 'WID-200', 'Widget Pro', 'Widget with the pro finish', 14.00, 'West', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (3, 'GAD-100', 'Gadget', 'Pocket gadget', 4.00, 'East', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (4, 'GAD-200', 'Gadget XL', 'Large gadget', 7.25, 'East', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (5, 'SPR-010', 'Sprocket', 'Steel sprocket', 2.10, 'West', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (6, 'SPR-020', 'Sprocket Twin', 'Paired sprockets', 3.80, 'West', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (7, 'BOL-005', 'Bolt M5', 'Box of 50', 5.00, 'East', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (8, 'BOL-008', 'Bolt M8', 'Box of 25', 6.40, 'East', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (9, 'NUT-005', 'Nut M5', 'Box of 100', 3.20, 'West', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (10, 'NUT-008', 'Nut M8', 'Box of 50', 3.90, 'West', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (11, 'GEA-030', 'Gear 30T', '30-tooth gear', 11.75, 'East', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (12, 'GEA-060', 'Gear 60T', '60-tooth gear', 18.20, 'East', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (13, 'BRK-001', 'Bracket', 'Angle bracket', 1.95, 'West', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (14, 'BRK-002', 'Bracket Heavy', 'Heavy angle bracket', 3.45, 'West', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (15, 'HIN-001', 'Hinge', 'Brass hinge', 4.60, 'East', TRUE);
INSERT INTO sku (id, code, name, description, price, region, is_active) VALUES (16, 'OLD-001', 'Legacy Widget', 'Retired widget', 8.00, 'West', FALSE);
INSERT INTO tag (id, name) VALUES (1, 'Rush');
INSERT INTO tag (id, name) VALUES (2, 'Gift');
INSERT INTO tag (id, name) VALUES (3, 'Wholesale');
INSERT INTO tag (id, name) VALUES (4, 'Backorder');
INSERT INTO tag (id, name) VALUES (5, 'VIP');
INSERT INTO sales_order (id, order_no, customer_id, region, order_date) VALUES (1, 'SO-1001', 1, 'West', '2026-10-01');
INSERT INTO sales_order (id, order_no, customer_id, region, order_date) VALUES (2, 'SO-1002', 2, 'East', '2026-10-05');
INSERT INTO sales_order (id, order_no, customer_id, region, order_date) VALUES (3, 'SO-1003', NULL, 'West', '2026-10-12');
INSERT INTO sales_order_line (id, sales_order_id, sku_id, quantity, unit_price) VALUES (1, 1, 1, 4, 9.50);
INSERT INTO sales_order_line (id, sales_order_id, sku_id, quantity, unit_price) VALUES (2, 1, 5, 10, 2.10);
INSERT INTO sales_order_line (id, sales_order_id, sku_id, quantity, unit_price) VALUES (3, 2, 3, 2, 4.00);
INSERT INTO sales_order_tag (id, sales_order_id, tag_id) VALUES (1, 1, 1);
INSERT INTO sales_order_tag (id, sales_order_id, tag_id) VALUES (2, 2, 3);
ALTER TABLE customer ALTER COLUMN id RESTART WITH 100;
ALTER TABLE sku ALTER COLUMN id RESTART WITH 100;
ALTER TABLE tag ALTER COLUMN id RESTART WITH 100;
ALTER TABLE sales_order ALTER COLUMN id RESTART WITH 100;
ALTER TABLE sales_order_line ALTER COLUMN id RESTART WITH 100;
ALTER TABLE sales_order_tag ALTER COLUMN id RESTART WITH 100;


DROP TABLE IF EXISTS place_address;
DROP TABLE IF EXISTS state_city;
DROP TABLE IF EXISTS country_state;
DROP TABLE IF EXISTS country;
CREATE TABLE country (id INTEGER AUTO_INCREMENT PRIMARY KEY, name VARCHAR(80));
CREATE TABLE country_state (id INTEGER AUTO_INCREMENT PRIMARY KEY, country_id INTEGER, name VARCHAR(80));
CREATE TABLE state_city (id INTEGER AUTO_INCREMENT PRIMARY KEY, state_id INTEGER, name VARCHAR(80));
CREATE TABLE place_address (id INTEGER AUTO_INCREMENT PRIMARY KEY, label VARCHAR(80), country_id INTEGER, state_id INTEGER, city_id INTEGER);
INSERT INTO country (id, name) VALUES (1, 'USA'), (2, 'Canada');
INSERT INTO country_state (id, country_id, name) VALUES (1, 1, 'Illinois'), (2, 1, 'Missouri'), (3, 2, 'Ontario'), (4, 2, 'Quebec');
INSERT INTO state_city (id, state_id, name) VALUES (1, 1, 'Chester'), (2, 1, 'Springfield'), (3, 2, 'St. Louis'), (4, 2, 'Kansas City'), (5, 3, 'Toronto'), (6, 3, 'Ottawa'), (7, 4, 'Montreal'), (8, 4, 'Quebec City');
INSERT INTO place_address (id, label, country_id, state_id, city_id) VALUES (1, 'USA address', 1, 1, 1), (2, 'Canada address', 2, 3, 5);
