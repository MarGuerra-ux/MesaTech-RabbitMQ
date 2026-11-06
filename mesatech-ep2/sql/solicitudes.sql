-- =====================================================================
-- MesaTech Cloud · EP2 · RDS SOLICITUDES (instancia exclusiva mesatech-rds-solicitudes)
-- Ejecutar desde la EC2 B (única con acceso por security group), con el usuario maestro:
--   CLAVE_APP='clave_real' ; sed "s/__CLAVE_APP__/$CLAVE_APP/" solicitudes.sql \
--     | mysql -h <endpoint-rds-solicitudes> -u admin -p
-- La clave real NO se guarda en el repositorio.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS solicitudes_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'solicitudes_app'@'10.0.%' IDENTIFIED BY '__CLAVE_APP__';
GRANT SELECT, INSERT, UPDATE, DELETE ON solicitudes_db.* TO 'solicitudes_app'@'10.0.%';
FLUSH PRIVILEGES;

USE solicitudes_db;

-- Copia local de categorías recibidas por RabbitMQ.
-- categoria_id = ID ORIGINAL de Catálogo (no autogenerado) para conservar la referencia de origen.
CREATE TABLE IF NOT EXISTS categoria_local (
  categoria_id     BIGINT       NOT NULL,
  nombre           VARCHAR(100) NOT NULL,
  event_id         VARCHAR(36)  NOT NULL,
  fecha_evento     VARCHAR(40)  NOT NULL,
  fecha_recepcion  DATETIME(6)  NOT NULL,
  PRIMARY KEY (categoria_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS solicitud (
  id                   BIGINT        NOT NULL AUTO_INCREMENT,
  titulo               VARCHAR(150)  NOT NULL,
  descripcion          VARCHAR(1000) NOT NULL,
  categoria_id         BIGINT        NOT NULL,
  estado               VARCHAR(20)   NOT NULL,
  solicitante          VARCHAR(150)  NOT NULL,
  fecha_creacion       DATETIME(6)   NOT NULL,
  fecha_actualizacion  DATETIME(6)   NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_solicitud_categoria_local
    FOREIGN KEY (categoria_id) REFERENCES categoria_local (categoria_id)
) ENGINE=InnoDB;

SHOW TABLES;
