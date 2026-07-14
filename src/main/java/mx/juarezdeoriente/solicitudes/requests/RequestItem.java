package mx.juarezdeoriente.solicitudes.requests;

import jakarta.persistence.*;
import mx.juarezdeoriente.solicitudes.shared.exception.DomainException;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "request_items")
public class RequestItem {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

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

    protected RequestItem() {}

    public static RequestItem create(UUID workerId, String description,
                                     BigDecimal quantity, String unit,
                                     BigDecimal unitCost, int position) {
        if (description == null || description.isBlank())
            throw new DomainException("La descripción del renglón es obligatoria");

        RequestItem item = new RequestItem();
        item.id          = UUID.randomUUID();
        item.workerId    = workerId;
        item.description = description.trim();
        item.quantity    = quantity;
        item.unit        = unit;
        item.unitCost    = unitCost;
        item.position    = position;
        return item;
    }

    public void update(UUID workerId, String description,
                       BigDecimal quantity, String unit, BigDecimal unitCost) {
        if (description == null || description.isBlank())
            throw new DomainException("La descripción del artículo es obligatoria");
        this.workerId    = workerId;
        this.description = description.trim();
        this.quantity    = quantity;
        this.unit        = unit;
        this.unitCost    = unitCost;
    }

    public BigDecimal getTotal() {
        if (quantity == null || unitCost == null) return BigDecimal.ZERO;
        return quantity.multiply(unitCost);
    }

    // package-private setter for parent relationship
    void setRequest(Request r) { this.request = r; }

    public UUID getId()             { return id; }
    public UUID getWorkerId()       { return workerId; }
    public String getDescription()  { return description; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit()         { return unit; }
    public BigDecimal getUnitCost() { return unitCost; }
    public int getPosition()        { return position; }
}
