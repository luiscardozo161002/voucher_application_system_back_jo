package mx.juarezdeoriente.solicitudes.workers.domain.event;

import mx.juarezdeoriente.solicitudes.shared.domain.model.DomainEvent;

import java.util.UUID;

public class WorkerCreatedEvent extends DomainEvent {

    private final UUID workerId;
    private final String name;
    private final UUID actorId;

    public WorkerCreatedEvent(UUID workerId, String name, UUID actorId) {
        super();
        this.workerId = workerId;
        this.name     = name;
        this.actorId  = actorId;
    }

    public UUID getWorkerId() { return workerId; }
    public String getName()   { return name; }
    public UUID getActorId()  { return actorId; }
}
