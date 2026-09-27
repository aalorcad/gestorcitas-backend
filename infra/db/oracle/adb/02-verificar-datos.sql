-- =====================================================================
-- Ejecutar como ADMIN DESPUÉS de levantar los microservicios.
-- Muestra que cada microservicio persiste en su propio esquema.
-- =====================================================================

-- Tablas creadas por cada microservicio
SELECT owner AS esquema, table_name AS tabla
FROM all_tables
WHERE owner IN ('CATALOGO', 'USUARIOS', 'CITAS')
ORDER BY owner, table_name;

-- Datos de ejemplo
SELECT id, nombre, valor_consulta, activa FROM CATALOGO.ESPECIALIDADES ORDER BY id;
SELECT id, nombre, email, activo, oid FROM USUARIOS.USUARIOS ORDER BY id;
SELECT usuario_id, rol FROM USUARIOS.USUARIO_ROLES ORDER BY usuario_id;
SELECT id, paciente_nombre, medico_nombre, especialidad_nombre, fecha_hora, estado FROM CITAS.CITAS ORDER BY fecha_hora DESC;
