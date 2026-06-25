package mx.juarezdeoriente.solicitudes.requests.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "request_items")
public class RequestItemJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private RequestJpaEntity request;

    @Column(name = "worker_id", columnDefinition = "uuid")
    private UUID workerId;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(precision = 14, scale = 4)
    private BigDecimal quantity;

    @Column(length = 30)
    private String unit;

    @Column(name = "unit_cost", precision = 14, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false)
    private int position;

    protected RequestItemJpaEntity() {}

    public UUID getId()                      { return id; }
    public void setId(UUID v)                { this.id = v; }
    public RequestJpaEntity getRequest()     { return request; }
    public void setRequest(RequestJpaEntity r){ this.request = r; }
    public UUID getWorkerId()                { return workerId; }
    public void setWorkerId(UUID v)          { this.workerId = v; }
    public String getDescription()           { return description; }
    public void setDescription(String v)     { this.description = v; }
    public BigDecimal getQuantity()          { return quantity; }
    public void setQuantity(BigDecimal v)    { this.quantity = v; }
    public String getUnit()                  { return unit; }
    public void setUnit(String v)            { this.unit = v; }
    public BigDecimal getUnitCost()          { return unitCost; }
    public void setUnitCost(BigDecimal v)    { this.unitCost = v; }
    public int getPosition()                 { return position; }
    public void setPosition(int v)           { this.position = v; }
}
