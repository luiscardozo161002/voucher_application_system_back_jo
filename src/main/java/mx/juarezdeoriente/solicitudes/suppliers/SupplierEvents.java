package mx.juarezdeoriente.solicitudes.suppliers;

import java.util.UUID;

/** Eventos simples de Spring publicados por SupplierService para auditoría. */
public class SupplierEvents {

    public record Created(UUID supplierId, String code, String name, UUID actorId) {}
    public record Updated(UUID supplierId, String name, UUID actorId) {}
    public record Deleted(UUID supplierId, String code, String name, UUID actorId) {}

    private SupplierEvents() {}
}
