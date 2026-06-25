package mx.juarezdeoriente.solicitudes.auth.application.port.in;

import java.util.UUID;

public interface ChangePasswordUseCase {

    record Command(UUID userId, String currentPassword, String newPassword) {}

    void execute(Command command);
}
