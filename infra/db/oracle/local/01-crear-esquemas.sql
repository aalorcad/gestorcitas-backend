-- =====================================================================
-- Oracle Database Free LOCAL (contenedor gvenzl/oracle-free, perfil
-- "oracle-local" de docker-compose). Se ejecuta automáticamente la primera
-- vez que se crea el contenedor. Replica los 3 esquemas de la Autonomous DB.
-- =====================================================================
ALTER SESSION SET CONTAINER = FREEPDB1;

CREATE USER CATALOGO IDENTIFIED BY "Catalogo#Local2026" QUOTA UNLIMITED ON USERS;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO CATALOGO;

CREATE USER USUARIOS IDENTIFIED BY "Usuarios#Local2026" QUOTA UNLIMITED ON USERS;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO USUARIOS;

CREATE USER CITAS IDENTIFIED BY "Citas#Local2026" QUOTA UNLIMITED ON USERS;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO CITAS;
