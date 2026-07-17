package mx.juarezdeoriente.solicitudes.suppliers;
import mx.juarezdeoriente.solicitudes.suppliers.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    boolean existsByCode(String code);

    Optional<Supplier> findByCode(String code);

    @Query("""
            SELECT s FROM Supplier s
            WHERE (:pattern IS NULL OR LOWER(s.code) LIKE :pattern
                                    OR LOWER(s.name) LIKE :pattern)
              AND (:active IS NULL OR s.active = :active)
            """)
    Page<Supplier> search(@Param("pattern") String pattern,
                          @Param("active") Boolean active,
                          Pageable pageable);
}
