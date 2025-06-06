-- A 100 record random sample from https://github.com/quarkusio/quarkus-super-heroes/blob/characterdata/all-heroes.sql
INSERT INTO customer(id, customer_name, customer_address)
VALUES (QUARKUS.CUSTOMER_SEQ.NEXTVAL, 'John Doe', '123 Main St, Springfield, USA');
