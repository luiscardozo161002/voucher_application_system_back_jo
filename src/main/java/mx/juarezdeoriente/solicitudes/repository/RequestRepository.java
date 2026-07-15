package mx.juarezdeoriente.solicitudes.repository;
import mx.juarezdeoriente.solicitudes.model.RequestStatus;
import mx.juarezdeoriente.solicitudes.model.Request;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID>,
                                            JpaSpecificationExecutor<Request> {

    Optional<Request> findByFolio(long folio);
}
