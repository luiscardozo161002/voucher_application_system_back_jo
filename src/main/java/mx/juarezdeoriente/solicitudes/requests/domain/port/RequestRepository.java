package mx.juarezdeoriente.solicitudes.requests.domain.port;

import mx.juarezdeoriente.solicitudes.requests.domain.model.Request;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestStatus;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RequestRepository {

    Request save(Request request);

    Optional<Request> findById(UUID id);

    Optional<Request> findByFolio(long folio);

    PageResult<Request> search(String folio, UUID supplierId, UUID workerId,
                               Instant from, Instant to, RequestStatus status,
                               UUID createdBy, int page, int size);

    /** Obtiene el siguiente folio de forma atómica (usando la secuencia de BD). */
    long nextFolio();
}
