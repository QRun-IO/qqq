-- Canonical Person/pets/notes schema and seed data for disposable mysql acceptance.
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


