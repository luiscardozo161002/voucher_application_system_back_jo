package mx.juarezdeoriente.solicitudes.suppliers.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SupplierJpaRepository extends JpaRepository<SupplierJpaEntity, UUID> {

    Optional<SupplierJpaEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
            SELECT s FROM SupplierJpaEntity s
            WHERE (:pattern IS NULL OR LOWER(s.code) LIKE :pattern
                                    OR LOWER(s.name) LIKE :pattern)
              AND (:active IS NULL OR s.active = :active)
            """)
    Page<SupplierJpaEntity> search(@Param("pattern") String pattern,
                                   @Param("active") Boolean active,
                                   Pageable pageable);
}