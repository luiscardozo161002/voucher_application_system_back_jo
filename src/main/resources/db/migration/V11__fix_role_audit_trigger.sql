CREATE OR REPLACE FUNCTION fn_role_audit() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO role_audit_log(user_id, role, action)
        VALUES (NEW.user_id, NEW.role, 'INSERT');
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO role_audit_log(user_id, role, action)
        VALUES (OLD.user_id, OLD.role, 'DELETE');
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
