package mx.juarezdeoriente.solicitudes.shared.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base para agregados. Acumula eventos de dominio; el servicio de aplicación
 * los publica con ApplicationEventPublisher tras persistir el agregado.
 */
public abstract class AggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> snapshot = List.copyOf(domainEvents); // copia inmutable antes de limpiar
        domainEvents.clear();
        return snapshot;
    }
}
