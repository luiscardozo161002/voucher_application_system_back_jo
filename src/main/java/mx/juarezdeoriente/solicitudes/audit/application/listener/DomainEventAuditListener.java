package mx.juarezdeoriente.solicitudes.audit.application.listener;

import mx.juarezdeoriente.solicitudes.audit.domain.model.AuditEvent;
import mx.juarezdeoriente.solicitudes.audit.domain.port.AuditEventRepository;
import mx.juarezdeoriente.solicitudes.auth.domain.event.UserCreatedEvent;
import mx.juarezdeoriente.solicitudes.auth.domain.event.UserDeactivatedEvent;
import mx.juarezdeoriente.solicitudes.auth.domain.event.UserDeletedEvent;
import mx.juarezdeoriente.solicitudes.auth.domain.event.UserUpdatedEvent;
import mx.juarezdeoriente.solicitudes.auth.domain.port.UserRepository;
import mx.juarezdeoriente.solicitudes.requests.domain.event.RequestCancelledEvent;
import mx.juarezdeoriente.solicitudes.requests.domain.event.RequestIssuedEvent;
import mx.juarezdeoriente.solicitudes.suppliers.domain.port.SupplierRepository;
import mx.juarezdeoriente.solicitudes.suppliers.domain.event.SupplierCreatedEvent;
import mx.juarezdeoriente.solicitudes.suppliers.domain.event.SupplierDeletedEvent;
import mx.juarezdeoriente.solicitudes.suppliers.domain.event.SupplierUpdatedEvent;
import mx.juarezdeoriente.solicitudes.workers.domain.event.WorkerCreatedEvent;
import mx.juarezdeoriente.solicitudes.workers.domain.event.WorkerDeletedEvent;
import mx.juarezdeoriente.solicitudes.workers.domain.event.WorkerUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Escucha todos los domain events y los persiste como registros de auditoría.
 * El patron Observable se materializa aquí: los agregados publican eventos y
 * este listener reacciona sin que el dominio lo conozca.
 */
@Component
public class DomainEventAuditListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventAuditListener.class);
    private final AuditEventRepository auditEventRepository;
    private final UserRepository       userRepository;
    private final SupplierRepository   supplierRepository;

    public DomainEventAuditListener(AuditEventRepository auditEventRepository,
                                    UserRepository userRepository,
                                    SupplierRepository supplierRepository) {
        this.auditEventRepository = auditEventRepository;
        this.userRepository       = userRepository;
        this.supplierRepository   = supplierRepository;
    }

    @EventListener
    public void on(UserCreatedEvent event) {
        save(null, "USER_CREATED", "User",
                event.getUserId().toString(),
                "username=" + event.getUsername());
    }

    @EventListener
    public void on(UserDeactivatedEvent event) {
        save(null, "USER_DEACTIVATED", "User",
                event.getUserId().toString(),
                "username=" + event.getUsername());
    }

    @EventListener
    public void on(UserUpdatedEvent event) {
        save(event.getActorId(), "USER_UPDATED", "User",
                event.getUserId().toString(),
                "username=" + event.getUsername());
    }

    @EventListener
    public void on(UserDeletedEvent event) {
        save(event.getActorId(), "USER_DELETED", "User",
                event.getUserId().toString(),
                "username=" + event.getUsername());
    }

    @EventListener
    public void on(SupplierCreatedEvent event) {
        save(event.getActorId(), "SUPPLIER_CREATED", "Supplier",
                event.getSupplierId().toString(),
                "code=" + event.getCode() + ",name=" + event.getName());
    }

    @EventListener
    public void on(SupplierUpdatedEvent event) {
        save(event.getActorId(), "SUPPLIER_UPDATED", "Supplier",
                event.getSupplierId().toString(),
                "name=" + event.getName());
    }

    @EventListener
    public void on(SupplierDeletedEvent event) {
        save(event.getActorId(), "SUPPLIER_DELETED", "Supplier",
                event.getSupplierId().toString(),
                "code=" + event.getCode() + ",name=" + event.getName());
    }

    @EventListener
    public void on(WorkerCreatedEvent event) {
        save(event.getActorId(), "WORKER_CREATED", "Worker",
                event.getWorkerId().toString(),
                "name=" + event.getName());
    }

    @EventListener
    public void on(WorkerUpdatedEvent event) {
        save(event.getActorId(), "WORKER_UPDATED", "Worker",
                event.getWorkerId().toString(),
                "name=" + event.getName());
    }

    @EventListener
    public void on(WorkerDeletedEvent event) {
        save(event.getActorId(), "WORKER_DELETED", "Worker",
                event.getWorkerId().toString(),
                "employeeNumber=" + event.getEmployeeNumber() + ",name=" + event.getName());
    }

    @EventListener
    public void on(RequestIssuedEvent event) {
        String folioStr = String.format("%07d", event.getFolio());
        String supplierName = supplierRepository.findById(event.getSupplierId())
                .map(s -> s.getName()).orElse("—");
        save(event.getIssuedBy(), "REQUEST_ISSUED", "Request",
                event.getRequestId().toString(),
                "folio=" + folioStr + ",proveedor=" + supplierName);
    }

    @EventListener
    public void on(RequestCancelledEvent event) {
        String folioStr = event.getFolio() != null ? String.format("%07d", event.getFolio()) : "—";
        save(event.getCancelledBy(), "REQUEST_CANCELLED", "Request",
                event.getRequestId().toString(),
                "folio=" + folioStr + ",motivo=" + event.getReason());
    }

    private void save(UUID actorId, String action, String entityType,
                      String entityId, String changes) {
        try {
            String actorUsername = resolveUsername(actorId);
            String fullChanges = actorUsername != null
                    ? "actor=" + actorUsername + (changes != null ? "," + changes : "")
                    : changes;
            auditEventRepository.save(AuditEvent.create(actorId, action, entityType, entityId, fullChanges));
        } catch (Exception ex) {
            // La auditoría no debe interrumpir el flujo principal
            log.error("Error guardando evento de auditoría: action={} entity={}", action, entityId, ex);
        }
    }

    private String resolveUsername(UUID actorId) {
        if (actorId == null) return null;
        return userRepository.findById(actorId)
                .map(u -> u.getUsername())
                .orElse(actorId.toString());
    }
}
