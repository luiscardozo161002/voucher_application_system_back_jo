package mx.juarezdeoriente.solicitudes.repository;
import mx.juarezdeoriente.solicitudes.model.RequestDocument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestDocumentRepository extends JpaRepository<RequestDocument, UUID> {

    List<RequestDocument> findByRequestIdOrderByGeneratedAtDesc(UUID requestId);
}
