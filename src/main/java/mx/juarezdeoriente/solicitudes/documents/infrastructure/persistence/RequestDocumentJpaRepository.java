package mx.juarezdeoriente.solicitudes.documents.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestDocumentJpaRepository extends JpaRepository<RequestDocumentJpaEntity, UUID> {

    List<RequestDocumentJpaEntity> findByRequestIdOrderByGeneratedAtDesc(UUID requestId);
}
