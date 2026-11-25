-- =====================================================================
-- GestorCitaCloudNative · Oracle Autonomous Database · esquemas nuevos (EP3 corrección + EP5)
--
-- Dónde ejecutarlo: consola OCI > Autonomous Database > Database actions
--                   > SQL, conectado como ADMIN. Usa "Ejecutar script" (F5).
--
-- Reemplaza <CLAVE_NOTIFICACIONES> y <CLAVE_ANALITICA> por claves de 12-30 caracteres con
-- mayúscula, minúscula y número, y sin el nombre del usuario. Usa las mismas en el .env
-- (NOTIFICACIONES_DB_PASSWORD, ANALITICA_DB_PASSWORD).
--
-- Las TABLAS las crea cada microservicio al arrancar (spring.jpa.hibernate.ddl-auto=update).
-- =====================================================================

-- ms-notificaciones -> esquema NOTIFICACIONES
-- (AUDITORIA_USUARIOS, NOTIFICACIONES_ENVIADAS, COMPROBANTES)
CREATE USER NOTIFICACIONES IDENTIFIED BY "<CLAVE_NOTIFICACIONES>";
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO NOTIFICACIONES;
ALTER USER NOTIFICACIONES QUOTA UNLIMITED ON DATA;

-- ms-analitica (EP5, Kafka) -> esquema ANALITICA
CREATE USER ANALITICA IDENTIFIED BY "<CLAVE_ANALITICA>";
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO ANALITICA;
ALTER USER ANALITICA QUOTA UNLIMITED ON DATA;

-- Verificación: deben aparecer los 2 esquemas
SELECT username, account_status, created FROM dba_users WHERE username IN ('NOTIFICACIONES', 'ANALITICA');

-- Después de usar el sistema, para ver lo guardado:
-- SELECT * FROM NOTIFICACIONES.AUDITORIA_USUARIOS ORDER BY ID DESC;
-- SELECT * FROM NOTIFICACIONES.NOTIFICACIONES_ENVIADAS ORDER BY ID DESC;
-- SELECT * FROM NOTIFICACIONES.COMPROBANTES ORDER BY ID DESC;
