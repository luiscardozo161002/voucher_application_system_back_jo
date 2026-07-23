-- =============================================================
-- Script: Límites de tiempo para el usuario de aplicación
-- Ejecutar UNA SOLA VEZ como superusuario (postgres)
-- LOCAL:      pgAdmin → conectado como postgres
-- PRODUCCIÓN: Railway → servicio Postgres → pestaña Query
-- =============================================================
-- ANTES DE EJECUTAR: reemplaza los valores entre < >
-- =============================================================

-- Mata queries que tarden más de 10 segundos
ALTER ROLE <usuario_app> SET statement_timeout = '10s';

-- Mata transacciones idle de más de 30s (previene bloqueos de tablas)
ALTER ROLE <usuario_app> SET idle_in_transaction_session_timeout = '30s';
