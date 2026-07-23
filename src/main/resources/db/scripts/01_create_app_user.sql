-- =============================================================
-- Script: Crear usuario de aplicación
-- Ejecutar UNA SOLA VEZ como superusuario (postgres)
-- LOCAL:      pgAdmin → conectado como postgres
-- PRODUCCIÓN: Railway → servicio Postgres → pestaña Query
-- =============================================================
-- ANTES DE EJECUTAR: reemplaza los valores entre < >
-- =============================================================

CREATE USER <usuario_app> WITH PASSWORD '<contraseña_app>';

-- LOCAL (nombre de BD: solicitudes)
GRANT CONNECT ON DATABASE solicitudes TO <usuario_app>;

-- PRODUCCIÓN: comenta la línea de arriba y descomenta esta
-- Railway usa "railway" como nombre de base de datos
-- GRANT CONNECT ON DATABASE railway TO <usuario_app>;

GRANT USAGE ON SCHEMA public TO <usuario_app>;
