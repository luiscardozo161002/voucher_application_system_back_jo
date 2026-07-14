package mx.juarezdeoriente.solicitudes.workers;

import java.util.UUID;

public class WorkerEvents {

    public record Created(UUID workerId, String name, UUID actorId) {}

    public record Updated(UUID workerId, String name, UUID actorId) {}

    public record Deleted(UUID workerId, String employeeNumber, String name, UUID actorId) {}
}
