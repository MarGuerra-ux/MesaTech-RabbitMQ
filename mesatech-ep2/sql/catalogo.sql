-- =====================================================================
-- MesaTech Cloud · EP2 · RDS CATÁLOGO (instancia exclusiva mesatech-rds-catalogo)
-- Ejecutar desde la EC2 A (única con acceso por security group), con el usuario maestro:
--   CLAVE_APP='clave_real' ; sed "s/__CLAVE_APP__/$CLAVE_APP/" catalogo.sql \
--     | mysql -h <endpoint-rds-catalogo> -u admin -p
-- La clave real NO se guarda en el repositorio.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS catalogo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Usuario de aplicación: solo desde la VPC (10.0.x.x) y solo sobre catalogo_db
CREATE USER IF NOT EXISTS 'catalogo_app'@'10.0.%' IDENTIFIED BY '__CLAVE_APP__';
GRANT SELECT, INSERT, UPDATE, DELETE ON catalogo_db.* TO 'catalogo_app'@'10.0.%';
FLUSH PRIVILEGES;

USE catalogo_db;

CREATE TABLE IF NOT EXISTS categoria (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  nombre              VARCHAR(100) NOT NULL,
  descripcion         VARCHAR(255) NULL,
  activa              BOOLEAN      NOT NULL DEFAULT TRUE,
  event_id            VARCHAR(36)  NULL,
  estado_publicacion  VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
  fecha_creacion      DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_categoria_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS prioridad (
  id      BIGINT      NOT NULL AUTO_INCREMENT,
  nombre  VARCHAR(50) NOT NULL,
  nivel   INT         NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_prioridad_nombre (nombre)
) ENGINE=InnoDB;

INSERT IGNORE INTO prioridad (nombre, nivel) VALUES
  ('Baja', 1), ('Media', 2), ('Alta', 3), ('Crítica', 4);

-- Verificación
SHOW TABLES;
SELECT * FROM prioridad;
