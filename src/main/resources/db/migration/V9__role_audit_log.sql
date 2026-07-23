CREATE TABLE IF NOT EXISTS role_audit_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    action      VARCHAR(10)  NOT NULL, -- 'INSERT' | 'DELETE'
    changed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    changed_by  VARCHAR(100) NOT NULL DEFAULT current_user
);

CREATE OR REPLACE FUNCTION fn_role_audit() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO role_audit_log(user_id, role, action)
        VALUES (NEW.user_id, NEW.roles, 'INSERT');
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO role_audit_log(user_id, role, action)
        VALUES (OLD.user_id, OLD.roles, 'DELETE');
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_role_audit ON user_roles;
CREATE TRIGGER trg_role_audit
    AFTER INSERT OR DELETE ON user_roles
    FOR EACH ROW EXECUTE FUNCTION fn_role_audit();
