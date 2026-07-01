package mx.juarezdeoriente.solicitudes.workers.domain.model;

import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.domain.model.AggregateRoot;
import mx.juarezdeoriente.solicitudes.workers.domain.event.WorkerCreatedEvent;
import mx.juarezdeoriente.solicitudes.workers.domain.event.WorkerUpdatedEvent;

import java.time.Instant;
import java.util.UUID;

public class Worker extends AggregateRoot {

    private final UUID id;
    private String companyCode;
    private String employeeNumber;
    private String name;
    private String phone;
    private WorkerType workerType;
    private boolean active;
    private final Instant createdAt;

    private Worker(UUID id, String companyCode, String employeeNumber,
                   String name, String phone, WorkerType workerType,
                   boolean active, Instant createdAt) {
        this.id             = id;
        this.companyCode    = companyCode;
        this.employeeNumber = employeeNumber;
        this.name           = name;
        this.phone          = phone;
        this.workerType     = workerType;
        this.active         = active;
        this.createdAt      = createdAt;
    }

    public static Worker reconstitute(UUID id, String companyCode, String employeeNumber,
                                      String name, String phone, WorkerType workerType,
                                      boolean active, Instant createdAt) {
        return new Worker(id, companyCode, employeeNumber, name, phone, workerType, active, createdAt);
    }

    public static Worker create(String companyCode, String employeeNumber,
                                String name, String phone, WorkerType workerType, UUID actorId) {
        if (name == null || name.isBlank()) throw new DomainException("El nombre del trabajador es obligatorio");
        if (workerType == null)             throw new DomainException("El tipo de trabajador es obligatorio");

        Worker w = new Worker(UUID.randomUUID(), companyCode, employeeNumber,
                name.trim(), phone, workerType, true, Instant.now());
        w.registerEvent(new WorkerCreatedEvent(w.id, w.name, actorId));
        return w;
    }

    public void update(String companyCode, String employeeNumber,
                       String name, String phone, WorkerType workerType, UUID actorId) {
        if (name == null || name.isBlank()) throw new DomainException("El nombre del trabajador es obligatorio");
        this.companyCode    = companyCode;
        this.employeeNumber = employeeNumber;
        this.name           = name.trim();
        this.phone          = phone;
        this.workerType     = workerType;
        registerEvent(new WorkerUpdatedEvent(this.id, this.name, actorId));
    }

    public void deactivate() { this.active = false; }
    public void activate()   { this.active = true; }

    public UUID getId()               { return id; }
    public String getCompanyCode()    { return companyCode; }
    public String getEmployeeNumber() { return employeeNumber; }
    public String getName()           { return name; }
    public String getPhone()          { return phone; }
    public WorkerType getWorkerType() { return workerType; }
    public boolean isActive()         { return active; }
    public Instant getCreatedAt()     { return createdAt; }
}
