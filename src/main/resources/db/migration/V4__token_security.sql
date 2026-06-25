-- =============================================================
-- V4 — Seguridad de tokens JWT
-- =============================================================

-- tokenVersion: al incrementarse invalida todos los tokens emitidos anteriormente.
-- Se incrementa en: cambio de contraseña y desactivación de cuenta.
ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

-- Tabla para refresh tokens persistidos (hash SHA-256, no el token en claro).
-- Permite rotación real y revocación en logout / cambio de contraseña.
CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_refresh_tokens_user    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash    ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
