package mx.juarezdeoriente.solicitudes.requests.infrastructure.web.dto;

import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestItem;

import java.math.BigDecimal;
import java.util.UUID;

public record RequestItemResponse(
        UUID id, UUID workerId, String description,
        BigDecimal quantity, String unit, BigDecimal unitCost,
        BigDecimal total, int position
) {
    public static RequestItemResponse from(RequestItem i) {
        return new RequestItemResponse(i.getId(), i.getWorkerId(), i.getDescription(),
                i.getQuantity(), i.getUnit(), i.getUnitCost(), i.getTotal(), i.getPosition());
    }
}
