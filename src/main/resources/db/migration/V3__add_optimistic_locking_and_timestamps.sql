-- =============================================================
-- V3 — Optimistic locking (version) y timestamps de modificacion
-- =============================================================

-- Columna version para control de concurrencia optimista
ALTER TABLE users     ADD COLUMN version    BIGINT NOT NULL DEFAULT 0;
ALTER TABLE suppliers ADD COLUMN version    BIGINT NOT NULL DEFAULT 0;
ALTER TABLE workers   ADD COLUMN version    BIGINT NOT NULL DEFAULT 0;
ALTER TABLE requests  ADD COLUMN version    BIGINT NOT NULL DEFAULT 0;

-- Timestamp de ultima modificacion
ALTER TABLE users     ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE suppliers ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE workers   ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE requests  ADD COLUMN updated_at TIMESTAMPTZ;

-- Inicializar updated_at con created_at para registros existentes
UPDATE users     SET updated_at = created_at;
UPDATE suppliers SET updated_at = created_at;
UPDATE workers   SET updated_at = created_at;
UPDATE requests  SET updated_at = created_at;
