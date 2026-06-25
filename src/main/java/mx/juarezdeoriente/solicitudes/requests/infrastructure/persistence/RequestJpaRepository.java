package mx.juarezdeoriente.solicitudes.requests.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

interface RequestJpaRepository extends JpaRepository<RequestJpaEntity, UUID>,
                                        JpaSpecificationExecutor<RequestJpaEntity> {

    Optional<RequestJpaEntity> findByFolio(long folio);
}