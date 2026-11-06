-- Consultas para capturas de evidencia (P1 a P7). Reemplazar <ID> por la categoría de la prueba.

-- ===== En la EC2 A contra RDS CATÁLOGO =====
-- mysql -h <endpoint-rds-catalogo> -u catalogo_app -p catalogo_db
SELECT @@hostname AS servidor_rds, DATABASE() AS base;
SELECT id, nombre, event_id, estado_publicacion, fecha_creacion FROM categoria ORDER BY id DESC LIMIT 10;
SELECT * FROM categoria WHERE id = <ID>;

-- ===== En la EC2 B contra RDS SOLICITUDES =====
-- mysql -h <endpoint-rds-solicitudes> -u solicitudes_app -p solicitudes_db
SELECT @@hostname AS servidor_rds, DATABASE() AS base;
SELECT categoria_id, nombre, event_id, fecha_evento, fecha_recepcion FROM categoria_local ORDER BY categoria_id DESC LIMIT 10;
SELECT * FROM categoria_local WHERE categoria_id = <ID>;
SELECT id, titulo, categoria_id, estado, solicitante, fecha_creacion FROM solicitud ORDER BY id DESC LIMIT 10;
