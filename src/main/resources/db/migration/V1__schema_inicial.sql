-- =============================================================
-- V1 — Esquema inicial del sistema de solicitudes
-- =============================================================

-- ---- Usuarios -----------------------------------------------

CREATE TABLE users (
    id                      UUID         PRIMARY KEY,
    username                VARCHAR(100) NOT NULL UNIQUE,
    password_hash           TEXT         NOT NULL,
    display_name            VARCHAR(200) NOT NULL,
    phone                   VARCHAR(20),
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    requires_password_reset BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts   INT          NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL
);

CREATE TABLE user_roles (
    user_id UUID        NOT NULL REFERENCES users(id),
    role    VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- ---- Proveedores --------------------------------------------

CREATE TABLE suppliers (
    id         UUID         PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    name       VARCHAR(300) NOT NULL,
    phone      VARCHAR(20),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_suppliers_code ON suppliers(code);
CREATE INDEX idx_suppliers_name ON suppliers(LOWER(name));

-- ---- Trabajadores -------------------------------------------

CREATE TABLE workers (
    id              UUID        PRIMARY KEY,
    company_code    VARCHAR(50),
    employee_number VARCHAR(50),
    name            VARCHAR(200) NOT NULL,
    phone           VARCHAR(20),
    worker_type     VARCHAR(20)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_workers_name ON workers(LOWER(name));

-- ---- Secuencia de folios (atómica, nunca reutilizable) ------
-- Arranca en 704 para continuar después del último folio legado (0000703)

CREATE SEQUENCE folio_seq START WITH 704 INCREMENT BY 1 NO CYCLE;

-- ---- Solicitudes --------------------------------------------

CREATE TABLE requests (
    id                   UUID        PRIMARY KEY,
    folio                BIGINT      UNIQUE,
    status               VARCHAR(20) NOT NULL,
    supplier_id          UUID        NOT NULL REFERENCES suppliers(id),
    destination          TEXT        NOT NULL,
    authorizer           VARCHAR(200),
    created_by           UUID        NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ NOT NULL,
    issued_at            TIMESTAMPTZ,
    cancelled_at         TIMESTAMPTZ,
    cancellation_reason  TEXT
);

CREATE INDEX idx_requests_folio       ON requests(folio);
CREATE INDEX idx_requests_supplier    ON requests(supplier_id);
CREATE INDEX idx_requests_created_by  ON requests(created_by);
CREATE INDEX idx_requests_status      ON requests(status);
CREATE INDEX idx_requests_created_at  ON requests(created_at DESC);

-- ---- Renglones de solicitud ---------------------------------

CREATE TABLE request_items (
    id          UUID           PRIMARY KEY,
    request_id  UUID           NOT NULL REFERENCES requests(id) ON DELETE CASCADE,
    worker_id   UUID           REFERENCES workers(id),
    description TEXT           NOT NULL,
    quantity    NUMERIC(14, 4),
    unit        VARCHAR(30),
    unit_cost   NUMERIC(14, 2),
    position    INTEGER       NOT NULL,
    UNIQUE (request_id, position)
);

CREATE INDEX idx_items_request  ON request_items(request_id);
CREATE INDEX idx_items_worker   ON request_items(worker_id);

-- ---- Bitácora de auditoría (solo inserción) -----------------

CREATE TABLE audit_events (
    id          BIGSERIAL    PRIMARY KEY,
    actor_id    UUID,
    action      VARCHAR(60)  NOT NULL,
    entity_type VARCHAR(60)  NOT NULL,
    entity_id   VARCHAR(100),
    changes     TEXT,
    ip_address  VARCHAR(50),
    occurred_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_audit_actor      ON audit_events(actor_id);
CREATE INDEX idx_audit_action     ON audit_events(action);
CREATE INDEX idx_audit_occurred   ON audit_events(occurred_at DESC);

-- Los usuarios iniciales son creados por DataSeeder al arrancar la aplicacion.
