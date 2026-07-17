package mx.juarezdeoriente.solicitudes.workers;
import mx.juarezdeoriente.solicitudes.workers.WorkerType;
import mx.juarezdeoriente.solicitudes.workers.Worker;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    @Query("""
            SELECT w FROM Worker w
            WHERE (:pattern IS NULL OR LOWER(w.name) LIKE :pattern
                                    OR LOWER(w.companyCode) LIKE :pattern
                                    OR LOWER(w.employeeNumber) LIKE :pattern)
              AND (:active IS NULL OR w.active = :active)
              AND (:workerType IS NULL OR w.workerType = :workerType)
            """)
    Page<Worker> search(@Param("pattern") String pattern,
                        @Param("active") Boolean active,
                        @Param("workerType") WorkerType workerType,
                        Pageable pageable);
}
