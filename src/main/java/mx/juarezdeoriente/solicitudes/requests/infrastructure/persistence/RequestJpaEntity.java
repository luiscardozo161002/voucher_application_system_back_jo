package mx.juarezdeoriente.solicitudes.requests.infrastructure.persistence;

import jakarta.persistence.*;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "requests")
public class RequestJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(unique = true)
    private Long folio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "supplier_id", nullable = false, columnDefinition = "uuid")
    private UUID supplierId;

    @Column(name = "solicitante_id", columnDefinition = "uuid")
    private UUID solicitanteId;

    @Column(nullable = false, columnDefinition = "text")
    private String destination;

    @Column(length = 200)
    private String authorizer;

    @Column(name = "created_by", nullable = false, columnDefinition = "uuid")
    private UUID createdBy;
    @Version
    private Long version;


    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<RequestItemJpaEntity> items = new ArrayList<>();

    protected RequestJpaEntity() {}

    public UUID getId()                      { return id; }
    public void setId(UUID v)                { this.id = v; }
    public Long getFolio()                   { return folio; }
    public void setFolio(Long v)             { this.folio = v; }
    public RequestStatus getStatus()         { return status; }
    public void setStatus(RequestStatus v)   { this.status = v; }
    public UUID getSupplierId()              { return supplierId; }
    public void setSupplierId(UUID v)        { this.supplierId = v; }
    public UUID getSolicitanteId()           { return solicitanteId; }
    public void setSolicitanteId(UUID v)     { this.solicitanteId = v; }
    public String getDestination()           { return destination; }
    public void setDestination(String v)     { this.destination = v; }
    public String getAuthorizer()            { return authorizer; }
    public void setAuthorizer(String v)      { this.authorizer = v; }
    public UUID getCreatedBy()               { return createdBy; }
    public void setCreatedBy(UUID v)         { this.createdBy = v; }
    public Instant getCreatedAt()            { return createdAt; }
    public Instant getUpdatedAt()        { return updatedAt; }
    public void setUpdatedAt(Instant v)  { this.updatedAt = v; }
    public Long getVersion()             { return version; }
    public void setVersion(Long v)       { this.version = v; }
    public void setCreatedAt(Instant v)      { this.createdAt = v; }
    public Instant getIssuedAt()             { return issuedAt; }
    public void setIssuedAt(Instant v)       { this.issuedAt = v; }
    public Instant getCancelledAt()          { return cancelledAt; }
    public void setCancelledAt(Instant v)    { this.cancelledAt = v; }
    public String getCancellationReason()    { return cancellationReason; }
    public void setCancellationReason(String v){ this.cancellationReason = v; }
    public List<RequestItemJpaEntity> getItems(){ return items; }
    public void setItems(List<RequestItemJpaEntity> v){ this.items = v; }
}
