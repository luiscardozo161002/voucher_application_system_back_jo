package mx.juarezdeoriente.solicitudes.requests.application.service;

import mx.juarezdeoriente.solicitudes.requests.domain.model.Request;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestItem;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestStatus;
import mx.juarezdeoriente.solicitudes.requests.domain.port.RequestRepository;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class RequestService {

    private final RequestRepository requestRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RequestService(RequestRepository requestRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.requestRepository = requestRepository;
        this.eventPublisher    = eventPublisher;
    }

    public Request createDraft(UUID supplierId, String destination,
                               String authorizer, UUID createdBy) {
        Request request = Request.createDraft(supplierId, destination, authorizer, createdBy);
        return requestRepository.save(request);
    }

    public Request updateDraft(UUID requestId, UUID supplierId,
                               String destination, String authorizer) {
        Request request = findById(requestId);
        request.updateDraft(supplierId, destination, authorizer);
        return requestRepository.save(request);
    }

    public Request addItem(UUID requestId, UUID workerId, String description,
                           BigDecimal quantity, String unit, BigDecimal unitCost) {
        Request request = findById(requestId);
        RequestItem item = RequestItem.create(workerId, description, quantity, unit,
                unitCost, request.getItems().size() + 1);
        request.addItem(item);
        return requestRepository.save(request);
    }

    public Request removeItem(UUID requestId, UUID itemId) {
        Request request = findById(requestId);
        request.removeItem(itemId);
        return requestRepository.save(request);
    }

    /**
     * Emite la solicitud. El folio se obtiene de la secuencia de BD dentro de
     * la misma transacción para garantizar unicidad.
     */
    public Request issue(UUID requestId) {
        Request request = findById(requestId);
        long folio      = requestRepository.nextFolio();
        request.issue(folio);
        Request saved = requestRepository.save(request);
        request.pullDomainEvents().forEach(eventPublisher::publishEvent);
        return saved;
    }

    public Request cancel(UUID requestId, String reason, UUID cancelledBy) {
        Request request = findById(requestId);
        request.cancel(reason, cancelledBy);
        Request saved = requestRepository.save(request);
        request.pullDomainEvents().forEach(eventPublisher::publishEvent);
        return saved;
    }

    @Transactional(readOnly = true)
    public Request findById(UUID id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitud", id));
    }

    @Transactional(readOnly = true)
    public PageResult<Request> search(String folio, UUID supplierId, UUID workerId,
                                      Instant from, Instant to, RequestStatus status,
                                      UUID createdBy, int page, int size) {
        return requestRepository.search(folio, supplierId, workerId,
                from, to, status, createdBy, page, size);
    }
}
