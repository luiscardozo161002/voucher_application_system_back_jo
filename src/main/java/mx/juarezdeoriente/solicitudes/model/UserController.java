package mx.juarezdeoriente.solicitudes.model;

import jakarta.validation.Valid;
import mx.juarezdeoriente.solicitudes.security.AppUserDetails;
import mx.juarezdeoriente.solicitudes.shared.PageResult;
import mx.juarezdeoriente.solicitudes.shared.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto.Response>> create(@Valid @RequestBody UserDto.CreateRequest request) {
        var user = userService.create(request.username(), request.password(),
                request.displayName(), request.phone(), request.roles());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(UserDto.Response.from(user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<UserDto.Response>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<UserDto.Response> result = userService.findAll(
                Math.max(0, page), Math.min(size < 1 ? 10 : size, 100)).map(UserDto.Response::from);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto.Response>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(UserDto.Response.from(userService.findById(id))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto.Response>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserDto.UpdateRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        var user = userService.update(id, request.displayName(), request.phone(),
                request.roles(), request.active(), principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(UserDto.Response.from(user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails principal) {
        userService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
