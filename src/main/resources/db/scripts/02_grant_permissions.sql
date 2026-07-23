-- =============================================================
-- Script: Permisos del usuario de aplicación
-- Ejecutar UNA SOLA VEZ como superusuario (postgres)
-- LOCAL:      pgAdmin → conectado como postgres
-- PRODUCCIÓN: Railway → servicio Postgres → pestaña Query
-- =============================================================
-- ANTES DE EJECUTAR: reemplaza los valores entre < >
-- =============================================================

-- Permisos de lectura/escritura en tablas existentes
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO <usuario_app>;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO <usuario_app>;

-- Flyway: el app solo lee el historial, no lo modifica
REVOKE INSERT, UPDATE, DELETE ON flyway_schema_history FROM <usuario_app>;

-- Seguridad: el app no puede modificar roles directamente
REVOKE INSERT, UPDATE, DELETE ON user_roles FROM <usuario_app>;

-- Permisos automáticos para tablas futuras creadas por Flyway
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO <usuario_app>;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO <usuario_app>;
