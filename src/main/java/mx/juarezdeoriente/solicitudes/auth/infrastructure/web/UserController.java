package mx.juarezdeoriente.solicitudes.auth.infrastructure.web;

import jakarta.validation.Valid;
import mx.juarezdeoriente.solicitudes.auth.application.port.in.CreateUserUseCase;
import mx.juarezdeoriente.solicitudes.auth.application.port.in.GetUsersUseCase;
import mx.juarezdeoriente.solicitudes.auth.application.port.in.UpdateUserUseCase;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto.UserRequest;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto.UserResponse;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto.UserUpdateRequest;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.shared.infrastructure.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUsersUseCase   getUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase,
                          GetUsersUseCase getUsersUseCase,
                          UpdateUserUseCase updateUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.getUsersUseCase   = getUsersUseCase;
        this.updateUserUseCase = updateUserUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        var user = createUserUseCase.execute(new CreateUserUseCase.Command(
                request.username(), request.password(),
                request.displayName(), request.phone(), request.roles()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(UserResponse.from(user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<UserResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<UserResponse> result = getUsersUseCase.findAll(page, size).map(UserResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(getUsersUseCase.findById(id))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {

        var user = updateUserUseCase.execute(new UpdateUserUseCase.Command(
                id, request.displayName(), request.phone(), request.roles(), request.active()
        ));
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }
}
