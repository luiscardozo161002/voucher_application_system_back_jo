package mx.juarezdeoriente.solicitudes.model;
import mx.juarezdeoriente.solicitudes.model.WorkerType;

import jakarta.persistence.*;
import mx.juarezdeoriente.solicitudes.exception.DomainException;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workers")
public class Worker {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "company_code", length = 50)
    private String companyCode;

    @Column(name = "employee_number", length = 50)
    private String employeeNumber;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "worker_type", nullable = false, length = 20)
    private WorkerType workerType;

    @Column(nullable = false)
    private boolean active;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Worker() {}

    public static Worker create(String companyCode, String employeeNumber,
                                String name, String phone, WorkerType workerType) {
        if (name == null || name.isBlank()) throw new DomainException("El nombre del trabajador es obligatorio");
        if (workerType == null)             throw new DomainException("El tipo de trabajador es obligatorio");

        Worker w = new Worker();
        w.id             = UUID.randomUUID();
        w.companyCode    = companyCode;
        w.employeeNumber = employeeNumber;
        w.name           = name.trim();
        w.phone          = phone;
        w.workerType     = workerType;
        w.active         = true;
        w.createdAt      = Instant.now();
        w.updatedAt      = w.createdAt;
        return w;
    }

    public void update(String companyCode, String employeeNumber,
                       String name, String phone, WorkerType workerType) {
        if (name == null || name.isBlank()) throw new DomainException("El nombre del trabajador es obligatorio");
        this.companyCode    = companyCode;
        this.employeeNumber = employeeNumber;
        this.name           = name.trim();
        this.phone          = phone;
        this.workerType     = workerType;
        this.updatedAt      = Instant.now();
    }

    public void toggleActive(boolean active) {
        this.active    = active;
        this.updatedAt = Instant.now();
    }

    public UUID getId()               { return id; }
    public String getCompanyCode()    { return companyCode; }
    public String getEmployeeNumber() { return employeeNumber; }
    public String getName()           { return name; }
    public String getPhone()          { return phone; }
    public WorkerType getWorkerType() { return workerType; }
    public boolean isActive()         { return active; }
    public Instant getCreatedAt()     { return createdAt; }
    public Instant getUpdatedAt()     { return updatedAt; }
}
