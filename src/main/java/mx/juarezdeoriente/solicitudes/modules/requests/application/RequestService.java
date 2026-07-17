package mx.juarezdeoriente.solicitudes.modules.requests.application;
import mx.juarezdeoriente.solicitudes.modules.requests.application.RequestEvents;
import mx.juarezdeoriente.solicitudes.modules.requests.application.dto.RequestDto;
import mx.juarezdeoriente.solicitudes.modules.requests.infrastructure.RequestSpecification;
import mx.juarezdeoriente.solicitudes.modules.requests.infrastructure.RequestRepository;
import mx.juarezdeoriente.solicitudes.modules.requests.domain.RequestStatus;
import mx.juarezdeoriente.solicitudes.modules.requests.domain.RequestItem;
import mx.juarezdeoriente.solicitudes.modules.requests.domain.Request;

import jakarta.persistence.EntityManager;
import mx.juarezdeoriente.solicitudes.exception.DomainException;
import mx.juarezdeoriente.solicitudes.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.shared.PageResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RequestService {

    private final RequestRepository        requestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager            em;

    public RequestService(RequestRepository requestRepository,
                          ApplicationEventPublisher eventPublisher,
                          EntityManager em) {
        this.requestRepository = requestRepository;
        this.eventPublisher    = eventPublisher;
        this.em                = em;
    }

    public Request createDraft(UUID supplierId, UUID solicitanteId,
                               String destination, String authorizer, UUID createdBy) {
        Request request = Request.createDraft(supplierId, solicitanteId, destination, authorizer, createdBy);
        return requestRepository.save(request);
    }

    public record ItemData(UUID workerId, String description, BigDecimal quantity, String unit, BigDecimal unitCost) {}

    public Request createAndIssue(UUID supplierId, UUID solicitanteId, String destination,
                                  String authorizer, UUID createdBy, List<ItemData> items) {
        Request request = Request.createDraft(supplierId, solicitanteId, destination, authorizer, createdBy);
        for (int i = 0; i < items.size(); i++) {
            ItemData item = items.get(i);
            request.addItem(RequestItem.create(item.workerId(), item.description(),
                    item.quantity(), item.unit(), item.unitCost(), i + 1));
        }
        long folio = nextFolio();
        request.issue(folio);
        Request issued = requestRepository.save(request);
        eventPublisher.publishEvent(new RequestEvents.Issued(issued.getId(), issued.getFolio(),
                issued.getSupplierId(), issued.getCreatedBy()));
        return issued;
    }

    public Request updateRequest(UUID requestId, UUID supplierId,
                                 String destination, String authorizer) {
        Request request = findById(requestId);
        if (request.getStatus() == RequestStatus.BORRADOR) {
            request.updateDraft(supplierId, destination, authorizer);
        } else {
            request.updateIssued(destination, authorizer);
        }
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

    public long countByStatus(RequestStatus status, UUID createdBy) {
        var spec = RequestSpecification.build(null, null, null, null, null, status, createdBy);
        return requestRepository.count(spec);
    }

    public Request restore(UUID requestId) {
        Request request = findById(requestId);
        request.restore();
        return requestRepository.save(request);
    }

    public void deleteAllCancelled(UUID createdBy) {
        var spec = RequestSpecification.build(null, null, null, null, null,
                RequestStatus.CANCELADA, createdBy, false);
        requestRepository.deleteAll(requestRepository.findAll(spec));
    }

    public void deletePermanently(UUID requestId) {
        Request request = findById(requestId);
        if (request.getStatus() != RequestStatus.CANCELADA) {
            throw new DomainException("Solo se pueden eliminar definitivamente solicitudes canceladas");
        }
        requestRepository.deleteById(requestId);
    }

    public Request updateItem(UUID requestId, UUID itemId, UUID workerId, String description,
                              BigDecimal quantity, String unit, BigDecimal unitCost) {
        Request request = findById(requestId);
        request.updateItem(itemId, workerId, description, quantity, unit, unitCost);
        return requestRepository.save(request);
    }

    public Request issue(UUID requestId) {
        Request request = findById(requestId);
        long folio      = nextFolio();
        request.issue(folio);
        Request saved = requestRepository.save(request);
        eventPublisher.publishEvent(new RequestEvents.Issued(saved.getId(), saved.getFolio(),
                saved.getSupplierId(), saved.getCreatedBy()));
        return saved;
    }

    public Request cancel(UUID requestId, String reason, UUID cancelledBy) {
        Request request = findById(requestId);
        request.cancel(reason, cancelledBy);
        Request saved = requestRepository.save(request);
        eventPublisher.publishEvent(new RequestEvents.Cancelled(saved.getId(), saved.getFolio(),
                cancelledBy, reason));
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
        return search(folio, supplierId, workerId, from, to, status, createdBy, page, size, false);
    }

    @Transactional(readOnly = true)
    public PageResult<Request> search(String folio, UUID supplierId, UUID workerId,
                                      Instant from, Instant to, RequestStatus status,
                                      UUID createdBy, int page, int size, boolean excludeCancelled) {
        String folioPattern = folio != null ? "%" + folio + "%" : null;
        var spec = RequestSpecification.build(folioPattern, supplierId, workerId,
                from, to, status, createdBy, excludeCancelled);
        Page<Request> p = requestRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(p.getContent(), page, size, p.getTotalElements());
    }

    private long nextFolio() {
        Number result = (Number) em.createNativeQuery(
                "SELECT COALESCE(MAX(folio), 0) + 1 FROM requests").getSingleResult();
        return result.longValue();
    }
}
