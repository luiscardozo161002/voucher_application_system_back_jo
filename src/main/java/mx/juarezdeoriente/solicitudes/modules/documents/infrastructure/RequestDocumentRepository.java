package mx.juarezdeoriente.solicitudes.modules.documents.infrastructure;
import mx.juarezdeoriente.solicitudes.modules.documents.domain.RequestDocument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestDocumentRepository extends JpaRepository<RequestDocument, UUID> {

    List<RequestDocument> findByRequestIdOrderByGeneratedAtDesc(UUID requestId);
}
