-- =============================================================
-- V5 — Idempotency keys y documentos PDF versionados
-- =============================================================

-- Tabla de idempotency keys.
-- El cliente envía Idempotency-Key: <uuid> en peticiones mutantes.
-- El servidor almacena el resultado y lo devuelve en llamadas repetidas.
CREATE TABLE idempotency_keys (
    id           UUID         PRIMARY KEY,
    key_hash     VARCHAR(64)  NOT NULL UNIQUE,   -- SHA-256 de "userId:key"
    endpoint     VARCHAR(200) NOT NULL,
    http_status  SMALLINT     NOT NULL,
    response     TEXT         NOT NULL,          -- JSON de la respuesta
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ  NOT NULL           -- TTL: 24h por defecto
);

CREATE INDEX idx_idempotency_hash    ON idempotency_keys(key_hash);
CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);

-- Tabla de documentos PDF generados por solicitud.
-- Permite auditar qué plantilla se usó y detectar cambios de contenido.
CREATE TABLE request_documents (
    id               UUID        PRIMARY KEY,
    request_id       UUID        NOT NULL REFERENCES requests(id),
    template_version VARCHAR(30) NOT NULL,
    file_size_bytes  INTEGER,
    checksum         VARCHAR(64),              -- SHA-256 del PDF
    generated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    generated_by     UUID        REFERENCES users(id)
);

CREATE INDEX idx_req_docs_request ON request_documents(request_id);
CREATE INDEX idx_req_docs_gen_at  ON request_documents(generated_at DESC);
