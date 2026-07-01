package mx.juarezdeoriente.solicitudes.auth.domain.event;

import mx.juarezdeoriente.solicitudes.shared.domain.model.DomainEvent;

import java.util.UUID;

public class UserDeletedEvent extends DomainEvent {

    private final UUID userId;
    private final String username;
    private final UUID actorId;

    public UserDeletedEvent(UUID userId, String username, UUID actorId) {
        super();
        this.userId   = userId;
        this.username = username;
        this.actorId  = actorId;
    }

    public UUID getUserId()    { return userId; }
    public String getUsername() { return username; }
    public UUID getActorId()   { return actorId; }
}
