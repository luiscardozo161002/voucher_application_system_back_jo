package mx.juarezdeoriente.solicitudes.event;

import java.util.UUID;

public class RequestEvents {

    public record Issued(UUID requestId, long folio, UUID supplierId, UUID issuedBy) {}

    public record Cancelled(UUID requestId, Long folio, UUID cancelledBy, String reason) {}
}
