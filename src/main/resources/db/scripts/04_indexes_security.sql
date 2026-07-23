-- =============================================================
-- Script: Índices para rendimiento y seguridad
-- Ejecutar UNA SOLA VEZ como superusuario (postgres)
-- Nota: estos índices también se aplican vía Flyway en V10
-- =============================================================

-- Refresh tokens: búsqueda rápida de tokens activos por usuario
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_active
    ON refresh_tokens(user_id) WHERE revoked = false;

-- Refresh tokens: limpieza diaria de expirados más rápida
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires
    ON refresh_tokens(expires_at) WHERE revoked = false;

-- Usuarios: detección rápida de cuentas bloqueadas
CREATE INDEX IF NOT EXISTS idx_users_locked_until
    ON users(locked_until) WHERE locked_until IS NOT NULL;
