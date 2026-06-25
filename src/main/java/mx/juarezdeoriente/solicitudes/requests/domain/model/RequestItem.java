package mx.juarezdeoriente.solicitudes.requests.domain.model;

import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;

import java.math.BigDecimal;
import java.util.UUID;

public class RequestItem {

    private final UUID id;
    private UUID workerId;
    private String description;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitCost;
    private int position;

    private RequestItem(UUID id, UUID workerId, String description,
                        BigDecimal quantity, String unit, BigDecimal unitCost, int position) {
        this.id          = id;
        this.workerId    = workerId;
        this.description = description;
        this.quantity    = quantity;
        this.unit        = unit;
        this.unitCost    = unitCost;
        this.position    = position;
    }

    public static RequestItem reconstitute(UUID id, UUID workerId, String description,
                                           BigDecimal quantity, String unit,
                                           BigDecimal unitCost, int position) {
        return new RequestItem(id, workerId, description, quantity, unit, unitCost, position);
    }

    public static RequestItem create(UUID workerId, String description,
                                     BigDecimal quantity, String unit,
                                     BigDecimal unitCost, int position) {
        if (description == null || description.isBlank())
            throw new DomainException("La descripción del renglón es obligatoria");

        return new RequestItem(UUID.randomUUID(), workerId, description,
                quantity, unit, unitCost, position);
    }

    /** Total calculado siempre en dominio; el cliente no lo envía como fuente de verdad. */
    public BigDecimal getTotal() {
        if (quantity == null || unitCost == null) return BigDecimal.ZERO;
        return quantity.multiply(unitCost);
    }

    public UUID getId()             { return id; }
    public UUID getWorkerId()       { return workerId; }
    public String getDescription()  { return description; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit()         { return unit; }
    public BigDecimal getUnitCost() { return unitCost; }
    public int getPosition()        { return position; }
}
