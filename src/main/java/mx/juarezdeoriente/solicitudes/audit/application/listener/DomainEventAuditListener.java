package mx.juarezdeoriente.solicitudes.audit.application.listener;

import mx.juarezdeoriente.solicitudes.audit.domain.model.AuditEvent;
import mx.juarezdeoriente.solicitudes.audit.domain.port.AuditEventRepository;
import mx.juarezdeoriente.solicitudes.auth.domain.event.UserCreatedEvent;
import mx.juarezdeoriente.solicitudes.auth.domain.event.UserDeactivatedEvent;
import mx.juarezdeoriente.solicitudes.requests.domain.event.RequestCancelledEvent;
import mx.juarezdeoriente.solicitudes.requests.domain.event.RequestIssuedEvent;
import mx.juarezdeoriente.solicitudes.suppliers.domain.event.SupplierCreatedEvent;
import mx.juarezdeoriente.solicitudes.suppliers.domain.event.SupplierUpdatedEvent;
import mx.juarezdeoriente.solicitudes.workers.domain.event.WorkerCreatedEvent;
import mx.juarezdeoriente.solicitudes.workers.domain.event.WorkerUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Escucha todos los domain events y los persiste como registros de auditoría.
 * El patron Observable se materializa aquí: los agregados publican eventos y
 * este listener reacciona sin que el dominio lo conozca.
 */
@Component
public class DomainEventAuditListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventAuditListener.class);
    private final AuditEventRepository auditEventRepository;

    public DomainEventAuditListener(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @EventListener
    @Async
    public void on(UserCreatedEvent event) {
        save(event.getUserId(), "USER_CREATED", "User",
                event.getUserId().toString(),
                "username=" + event.getUsername());
    }

    @EventListener
    @Async
    public void on(UserDeactivatedEvent event) {
        save(event.getUserId(), "USER_DEACTIVATED", "User",
                event.getUserId().toString(),
                "username=" + event.getUsername());
    }

    @EventListener
    @Async
    public void on(SupplierCreatedEvent event) {
        save(null, "SUPPLIER_CREATED", "Supplier",
                event.getSupplierId().toString(),
                "code=" + event.getCode() + ",name=" + event.getName());
    }

    @EventListener
    @Async
    public void on(WorkerCreatedEvent event) {
        save(null, "WORKER_CREATED", "Worker",
                event.getWorkerId().toString(),
                "name=" + event.getName());
    }

    @EventListener
    @Async
    public void on(SupplierUpdatedEvent event) {
        save(null, "SUPPLIER_UPDATED", "Supplier",
                event.getSupplierId().toString(),
                "name=" + event.getName());
    }

    @EventListener
    @Async
    public void on(WorkerUpdatedEvent event) {
        save(null, "WORKER_UPDATED", "Worker",
                event.getWorkerId().toString(),
                "name=" + event.getName());
    }

    @EventListener
    @Async
    public void on(RequestIssuedEvent event) {
        save(event.getIssuedBy(), "REQUEST_ISSUED", "Request",
                event.getRequestId().toString(),
                "folio=" + event.getFolio());
    }

    @EventListener
    @Async
    public void on(RequestCancelledEvent event) {
        save(event.getCancelledBy(), "REQUEST_CANCELLED", "Request",
                event.getRequestId().toString(),
                "folio=" + event.getFolio() + ",reason=" + event.getReason());
    }

    private void save(java.util.UUID actorId, String action, String entityType,
                      String entityId, String changes) {
        try {
            auditEventRepository.save(AuditEvent.create(actorId, action, entityType, entityId, changes));
        } catch (Exception ex) {
            // La auditoría no debe interrumpir el flujo principal
            log.error("Error guardando evento de auditoría: action={} entity={}", action, entityId, ex);
        }
    }
}
