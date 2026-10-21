CREATE TABLE sample (
   id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
   name VARCHAR(255)
);
INSERT INTO sample (name) VALUES ('First example');

CREATE TABLE orderDeskEntity (
   id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
   name VARCHAR(100) NOT NULL,
   description VARCHAR(500),
   status VARCHAR(50),
   isActive BOOLEAN,
   createDate DATETIME(3),
   modifyDate DATETIME(3)
);

CREATE TABLE orderDeskChildEntity (
   id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
   orderDeskEntityId INT NOT NULL,
   name VARCHAR(100) NOT NULL,
   description VARCHAR(500),
   sortOrder INT,
   isActive BOOLEAN,
   createDate DATETIME(3),
   modifyDate DATETIME(3),
   CONSTRAINT fk_orderdesk_parent FOREIGN KEY (orderDeskEntityId) REFERENCES orderDeskEntity(id)
);
