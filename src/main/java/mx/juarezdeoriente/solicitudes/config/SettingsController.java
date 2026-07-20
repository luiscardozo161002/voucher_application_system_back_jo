package mx.juarezdeoriente.solicitudes.config;

import mx.juarezdeoriente.solicitudes.shared.web.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone configuración pública de la empresa al frontend.
 * No requiere autenticación — solo datos no sensibles.
 */
@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final CompanyProperties company;

    public SettingsController(CompanyProperties company) {
        this.company = company;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.ok(new SettingsResponse(
                company.getName(),
                company.getAddress(),
                company.getCityLine(),
                company.getPostalCode(),
                company.getRfc(),
                company.getPhone(),
                company.getExtension(),
                company.getAuthorizers() != null ? company.getAuthorizers() : List.of()
        )));
    }

    public record SettingsResponse(
            String companyName,
            String companyAddress,
            String companyCityLine,
            String companyPostalCode,
            String companyRfc,
            String companyPhone,
            String companyExtension,
            List<String> authorizers
    ) {}
}
