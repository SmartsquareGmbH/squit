CREATE TABLE CARS (
  id    INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  speed INTEGER NOT NULL
);

INSERT INTO CARS (speed) VALUES (100);
INSERT INTO CARS (speed) VALUES (150);
