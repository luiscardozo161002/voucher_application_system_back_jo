package mx.juarezdeoriente.solicitudes.auth.domain.event;

import mx.juarezdeoriente.solicitudes.shared.domain.model.DomainEvent;

import java.util.UUID;

public class UserDeactivatedEvent extends DomainEvent {

    private final UUID userId;
    private final String username;

    public UserDeactivatedEvent(UUID userId, String username) {
        super();
        this.userId   = userId;
        this.username = username;
    }

    public UUID getUserId()    { return userId; }
    public String getUsername() { return username; }
}
