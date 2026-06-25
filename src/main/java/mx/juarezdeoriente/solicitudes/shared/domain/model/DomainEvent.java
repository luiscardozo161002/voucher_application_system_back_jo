package mx.juarezdeoriente.solicitudes.shared.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato base para todos los eventos de dominio.
 * Los listeners se registran con @EventListener en la capa de aplicación.
 */
public abstract class DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;

    protected DomainEvent() {
        this.eventId    = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
