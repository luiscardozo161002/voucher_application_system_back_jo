-- Índices para rendimiento en consultas de seguridad críticas

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_active
    ON refresh_tokens(user_id) WHERE revoked = false;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires
    ON refresh_tokens(expires_at) WHERE revoked = false;

CREATE INDEX IF NOT EXISTS idx_users_locked_until
    ON users(locked_until) WHERE locked_until IS NOT NULL;
