-- Permite borrar solicitudes sin violar la FK de request_documents
ALTER TABLE request_documents
    DROP CONSTRAINT IF EXISTS request_documents_request_id_fkey;

ALTER TABLE request_documents
    ADD CONSTRAINT request_documents_request_id_fkey
        FOREIGN KEY (request_id) REFERENCES requests(id) ON DELETE CASCADE;
