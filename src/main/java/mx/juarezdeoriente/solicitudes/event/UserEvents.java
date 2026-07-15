package mx.juarezdeoriente.solicitudes.event;

import java.util.UUID;

public class UserEvents {

    public record Created(UUID userId, String username) {}

    public record Deactivated(UUID userId, String username) {}

    public record Updated(UUID userId, String username, UUID actorId) {}

    public record Deleted(UUID userId, String username, UUID actorId) {}
}
