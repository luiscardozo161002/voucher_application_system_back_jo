package mx.juarezdeoriente.solicitudes.workers.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface WorkerJpaRepository extends JpaRepository<WorkerJpaEntity, UUID> {

    @Query("""
            SELECT w FROM WorkerJpaEntity w
            WHERE (:pattern IS NULL OR LOWER(w.name) LIKE :pattern
                                    OR LOWER(w.companyCode) LIKE :pattern
                                    OR LOWER(w.employeeNumber) LIKE :pattern)
              AND (:active IS NULL OR w.active = :active)
            """)
    Page<WorkerJpaEntity> search(@Param("pattern") String pattern,
                                 @Param("active") Boolean active,
                                 Pageable pageable);
}