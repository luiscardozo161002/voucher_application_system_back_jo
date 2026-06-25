package mx.juarezdeoriente.solicitudes.requests.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import mx.juarezdeoriente.solicitudes.requests.domain.model.Request;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestItem;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestStatus;
import mx.juarezdeoriente.solicitudes.requests.domain.port.RequestRepository;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class RequestRepositoryAdapter implements RequestRepository {

    private final RequestJpaRepository jpa;
    private final EntityManager        em;

    RequestRepositoryAdapter(RequestJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em  = em;
    }

    @Override
    public Request save(Request r) {
        RequestJpaEntity e = jpa.findById(r.getId()).orElse(new RequestJpaEntity());
        e.setId(r.getId());
        e.setFolio(r.getFolio());
        e.setStatus(r.getStatus());
        e.setSupplierId(r.getSupplierId());
        e.setDestination(r.getDestination());
        e.setAuthorizer(r.getAuthorizer());
        e.setCreatedBy(r.getCreatedBy());
        e.setCreatedAt(r.getCreatedAt());
        e.setIssuedAt(r.getIssuedAt());
        e.setCancelledAt(r.getCancelledAt());
        e.setCancellationReason(r.getCancellationReason());
        e.setUpdatedAt(java.time.Instant.now());

        e.getItems().clear();
        r.getItems().stream()
                .map(i -> toItemEntity(i, e))
                .forEach(e.getItems()::add);

        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<Request> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Request> findByFolio(long folio) {
        return jpa.findByFolio(folio).map(this::toDomain);
    }

    @Override
    public PageResult<Request> search(String folio, UUID supplierId, UUID workerId,
                                      Instant from, Instant to, RequestStatus status,
                                      UUID createdBy, int page, int size) {
        String folioPattern = folio != null ? "%" + folio + "%" : null;
        var spec = RequestSpecification.build(folioPattern, supplierId, workerId,
                from, to, status, createdBy);
        Page<RequestJpaEntity> p = jpa.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(p.getContent().stream().map(this::toDomain).toList(),
                page, size, p.getTotalElements());
    }

    @Override
    public long nextFolio() {
        // Usa la secuencia PostgreSQL definida en Flyway; atómica y no reutilizable
        return ((Number) em.createNativeQuery("SELECT nextval('folio_seq')").getSingleResult()).longValue();
    }

    // --- Mapeo dominio <-> JPA ---

    private RequestJpaEntity toEntity(Request r) {
        RequestJpaEntity e = new RequestJpaEntity();
        e.setId(r.getId());
        e.setFolio(r.getFolio());
        e.setStatus(r.getStatus());
        e.setSupplierId(r.getSupplierId());
        e.setDestination(r.getDestination());
        e.setAuthorizer(r.getAuthorizer());
        e.setCreatedBy(r.getCreatedBy());
        e.setCreatedAt(r.getCreatedAt());
        e.setIssuedAt(r.getIssuedAt());
        e.setCancelledAt(r.getCancelledAt());
        e.setCancellationReason(r.getCancellationReason());

        List<RequestItemJpaEntity> itemEntities = r.getItems().stream()
                .map(i -> toItemEntity(i, e))
                .toList();
        e.setItems(itemEntities);
        return e;
    }

    private RequestItemJpaEntity toItemEntity(RequestItem i, RequestJpaEntity parent) {
        RequestItemJpaEntity e = new RequestItemJpaEntity();
        e.setId(i.getId());
        e.setRequest(parent);
        e.setWorkerId(i.getWorkerId());
        e.setDescription(i.getDescription());
        e.setQuantity(i.getQuantity());
        e.setUnit(i.getUnit());
        e.setUnitCost(i.getUnitCost());
        e.setPosition(i.getPosition());
        return e;
    }

    private Request toDomain(RequestJpaEntity e) {
        List<RequestItem> items = e.getItems().stream()
                .map(i -> RequestItem.reconstitute(i.getId(), i.getWorkerId(),
                        i.getDescription(), i.getQuantity(), i.getUnit(),
                        i.getUnitCost(), i.getPosition()))
                .toList();

        return Request.reconstitute(
                e.getId(), e.getFolio(), e.getStatus(), e.getSupplierId(),
                e.getDestination(), e.getAuthorizer(), e.getCreatedBy(),
                items, e.getCreatedAt(), e.getIssuedAt(),
                e.getCancelledAt(), e.getCancellationReason()
        );
    }
}
