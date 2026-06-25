package mx.juarezdeoriente.solicitudes.workers.infrastructure.persistence;

import jakarta.persistence.*;
import mx.juarezdeoriente.solicitudes.workers.domain.model.WorkerType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workers")
public class WorkerJpaEntity {

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

    protected WorkerJpaEntity() {}

    public UUID getId()                      { return id; }
    public void setId(UUID v)                { this.id = v; }
    public String getCompanyCode()           { return companyCode; }
    public void setCompanyCode(String v)     { this.companyCode = v; }
    public String getEmployeeNumber()        { return employeeNumber; }
    public void setEmployeeNumber(String v)  { this.employeeNumber = v; }
    public String getName()                  { return name; }
    public void setName(String v)            { this.name = v; }
    public String getPhone()                 { return phone; }
    public void setPhone(String v)           { this.phone = v; }
    public WorkerType getWorkerType()        { return workerType; }
    public void setWorkerType(WorkerType v)  { this.workerType = v; }
    public boolean isActive()               { return active; }
    public void setActive(boolean v)         { this.active = v; }
    public Instant getCreatedAt()            { return createdAt; }
    public Instant getUpdatedAt()        { return updatedAt; }
    public void setUpdatedAt(Instant v)  { this.updatedAt = v; }
    public Long getVersion()             { return version; }
    public void setVersion(Long v)       { this.version = v; }
    public void setCreatedAt(Instant v)      { this.createdAt = v; }
}
