package mx.juarezdeoriente.solicitudes.event;
import mx.juarezdeoriente.solicitudes.repository.AuditRepository;
import mx.juarezdeoriente.solicitudes.model.AuditEvent;

import mx.juarezdeoriente.solicitudes.event.UserEvents;
import mx.juarezdeoriente.solicitudes.repository.UserRepository;
import mx.juarezdeoriente.solicitudes.event.RequestEvents;
import mx.juarezdeoriente.solicitudes.event.SupplierEvents;
import mx.juarezdeoriente.solicitudes.repository.SupplierRepository;
import mx.juarezdeoriente.solicitudes.event.WorkerEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Escucha todos los domain events y los persiste como registros de auditoría.
 */
@Component
public class AuditListener {

    private static final Logger log = LoggerFactory.getLogger(AuditListener.class);

    private final AuditRepository    auditRepository;
    private final UserRepository     userRepository;
    private final SupplierRepository supplierRepository;

    public AuditListener(AuditRepository auditRepository,
                         UserRepository userRepository,
                         SupplierRepository supplierRepository) {
        this.auditRepository    = auditRepository;
        this.userRepository     = userRepository;
        this.supplierRepository = supplierRepository;
    }

    @EventListener
    public void on(UserEvents.Created event) {
        save(null, "USER_CREATED", "User",
                event.userId().toString(),
                "username=" + event.username());
    }

    @EventListener
    public void on(UserEvents.Deactivated event) {
        save(null, "USER_DEACTIVATED", "User",
                event.userId().toString(),
                "username=" + event.username());
    }

    @EventListener
    public void on(UserEvents.Updated event) {
        save(event.actorId(), "USER_UPDATED", "User",
                event.userId().toString(),
                "username=" + event.username());
    }

    @EventListener
    public void on(UserEvents.Deleted event) {
        save(event.actorId(), "USER_DELETED", "User",
                event.userId().toString(),
                "username=" + event.username());
    }

    @EventListener
    public void on(SupplierEvents.Created event) {
        save(event.actorId(), "SUPPLIER_CREATED", "Supplier",
                event.supplierId().toString(),
                "code=" + event.code() + ",name=" + event.name());
    }

    @EventListener
    public void on(SupplierEvents.Updated event) {
        save(event.actorId(), "SUPPLIER_UPDATED", "Supplier",
                event.supplierId().toString(),
                "name=" + event.name());
    }

    @EventListener
    public void on(SupplierEvents.Deleted event) {
        save(event.actorId(), "SUPPLIER_DELETED", "Supplier",
                event.supplierId().toString(),
                "code=" + event.code() + ",name=" + event.name());
    }

    @EventListener
    public void on(WorkerEvents.Created event) {
        save(event.actorId(), "WORKER_CREATED", "Worker",
                event.workerId().toString(),
                "name=" + event.name());
    }

    @EventListener
    public void on(WorkerEvents.Updated event) {
        save(event.actorId(), "WORKER_UPDATED", "Worker",
                event.workerId().toString(),
                "name=" + event.name());
    }

    @EventListener
    public void on(WorkerEvents.Deleted event) {
        save(event.actorId(), "WORKER_DELETED", "Worker",
                event.workerId().toString(),
                "employeeNumber=" + event.employeeNumber() + ",name=" + event.name());
    }

    @EventListener
    public void on(RequestEvents.Issued event) {
        String folioStr = String.format("%07d", event.folio());
        String supplierName = supplierRepository.findById(event.supplierId())
                .map(s -> s.getName()).orElse("—");
        save(event.issuedBy(), "REQUEST_ISSUED", "Request",
                event.requestId().toString(),
                "folio=" + folioStr + ",proveedor=" + supplierName);
    }

    @EventListener
    public void on(RequestEvents.Cancelled event) {
        String folioStr = event.folio() != null ? String.format("%07d", event.folio()) : "—";
        save(event.cancelledBy(), "REQUEST_CANCELLED", "Request",
                event.requestId().toString(),
                "folio=" + folioStr + ",motivo=" + event.reason());
    }

    private void save(UUID actorId, String action, String entityType,
                      String entityId, String changes) {
        try {
            String actorUsername = resolveUsername(actorId);
            String fullChanges = actorUsername != null
                    ? "actor=" + actorUsername + (changes != null ? "," + changes : "")
                    : changes;
            auditRepository.save(AuditEvent.create(actorId, action, entityType, entityId, fullChanges));
        } catch (Exception ex) {
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
