package mx.juarezdeoriente.solicitudes.workers.domain.event;

import mx.juarezdeoriente.solicitudes.shared.domain.model.DomainEvent;

import java.util.UUID;

public class WorkerDeletedEvent extends DomainEvent {

    private final UUID workerId;
    private final String employeeNumber;
    private final String name;
    private final UUID actorId;

    public WorkerDeletedEvent(UUID workerId, String employeeNumber, String name, UUID actorId) {
        super();
        this.workerId        = workerId;
        this.employeeNumber  = employeeNumber;
        this.name            = name;
        this.actorId         = actorId;
    }

    public UUID getWorkerId()         { return workerId; }
    public String getEmployeeNumber() { return employeeNumber; }
    public String getName()           { return name; }
    public UUID getActorId()          { return actorId; }
}
