-- Guarda quién hace la solicitud (el trabajador que la firma).
-- Nullable para no romper solicitudes existentes.
ALTER TABLE requests
    ADD COLUMN solicitante_id UUID REFERENCES workers(id) ON DELETE SET NULL;
