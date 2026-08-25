ALTER TABLE role_audit_log
    ALTER COLUMN user_id TYPE UUID USING user_id::text::uuid;
