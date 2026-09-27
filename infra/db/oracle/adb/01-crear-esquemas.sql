-- =====================================================================
-- GestorCitaCloudNative · Oracle Autonomous Database
-- 1 instancia, 1 esquema (usuario) por microservicio.
--
-- Dónde ejecutarlo: consola OCI > Autonomous Database > Database actions
--                   > SQL, conectado como ADMIN. Usa "Ejecutar script" (F5).
--
-- Contraseñas: 12-30 caracteres, con mayúscula, minúscula y número, y sin
-- contener el nombre del usuario. Si las cambias, usa las mismas en el .env
-- (CATALOGO_DB_PASSWORD, USUARIOS_DB_PASSWORD, CITAS_DB_PASSWORD).
--
-- Las TABLAS no se crean aquí: cada microservicio las crea al arrancar
-- (spring.jpa.hibernate.ddl-auto=update) dentro de su propio esquema.
-- =====================================================================

-- ms-catalogo -> esquema CATALOGO (tabla ESPECIALIDADES)
CREATE USER CATALOGO IDENTIFIED BY "<CLAVE_CATALOGO>";
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO CATALOGO;
ALTER USER CATALOGO QUOTA UNLIMITED ON DATA;

-- ms-usuarios -> esquema USUARIOS (USUARIOS, USUARIO_ROLES, PERFILES_PACIENTE, PERFILES_MEDICO)
CREATE USER USUARIOS IDENTIFIED BY "<CLAVE_USUARIOS>";
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO USUARIOS;
ALTER USER USUARIOS QUOTA UNLIMITED ON DATA;

-- ms-citas -> esquema CITAS (tabla CITAS)
CREATE USER CITAS IDENTIFIED BY "<CLAVE_CITAS>";
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO CITAS;
ALTER USER CITAS QUOTA UNLIMITED ON DATA;

-- (Opcional) Permite entrar a Database Actions con cada esquema para ver sus tablas en la demo
BEGIN
  ORDS_ADMIN.ENABLE_SCHEMA(p_enabled => TRUE, p_schema => 'CATALOGO', p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => 'catalogo', p_auto_rest_auth => TRUE);
  ORDS_ADMIN.ENABLE_SCHEMA(p_enabled => TRUE, p_schema => 'USUARIOS', p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => 'usuarios', p_auto_rest_auth => TRUE);
  ORDS_ADMIN.ENABLE_SCHEMA(p_enabled => TRUE, p_schema => 'CITAS',    p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => 'citas',    p_auto_rest_auth => TRUE);
END;
/

-- Verificación: deben aparecer los 3 esquemas
SELECT username, account_status, created FROM dba_users WHERE username IN ('CATALOGO', 'USUARIOS', 'CITAS');
