package mx.juarezdeoriente.solicitudes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class SupplierDto {

    public record CreateRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 300) String name,
            String phone) {}

    public record UpdateRequest(
            @Size(max = 300) String name,
            String phone,
            Boolean active) {}

    public record Response(
            UUID id, String code, String name,
            String phone, boolean active, Instant createdAt) {

        public static Response from(Supplier s) {
            return new Response(s.getId(), s.getCode(), s.getName(),
                    s.getPhone(), s.isActive(), s.getCreatedAt());
        }
    }

    private SupplierDto() {}
}
