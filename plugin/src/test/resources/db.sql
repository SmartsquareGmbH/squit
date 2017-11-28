CREATE TABLE ANIMALS (
  id    INT IDENTITY PRIMARY KEY,
  color VARCHAR2(255) NOT NULL,
  name  VARCHAR2(255) NOT NULL
);

INSERT INTO ANIMALS (color, name) VALUES ('brown', 'dog');
INSERT INTO ANIMALS (color, name) VALUES ('black', 'cat');
INSERT INTO ANIMALS (color, name) VALUES ('green', 'sloth');
