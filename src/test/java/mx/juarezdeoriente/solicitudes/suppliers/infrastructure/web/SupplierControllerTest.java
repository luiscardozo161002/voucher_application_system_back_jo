package mx.juarezdeoriente.solicitudes.suppliers.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.juarezdeoriente.solicitudes.auth.security.AppUserDetailsService;
import mx.juarezdeoriente.solicitudes.auth.security.JwtAuthenticationFilter;
import mx.juarezdeoriente.solicitudes.auth.security.JwtService;
import mx.juarezdeoriente.solicitudes.auth.security.SecurityConfig;
import mx.juarezdeoriente.solicitudes.config.CorsConfig;
import mx.juarezdeoriente.solicitudes.shared.exception.ConflictException;
import mx.juarezdeoriente.solicitudes.shared.web.PageableDefaults;
import mx.juarezdeoriente.solicitudes.suppliers.Supplier;
import mx.juarezdeoriente.solicitudes.suppliers.SupplierController;
import mx.juarezdeoriente.solicitudes.suppliers.SupplierDto;
import mx.juarezdeoriente.solicitudes.suppliers.SupplierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SupplierController.class, properties = "app.rate-limit.enabled=false")
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, CorsConfig.class, PageableDefaults.class })
class SupplierControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean SupplierService       supplierService;
    @MockBean JwtService            jwtService;
    @MockBean AppUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_proveedor_valido_retorna_201() throws Exception {
        Supplier supplier = Supplier.create("PROV-TEST", "Proveedor Test", "555-0000");

        when(supplierService.create(anyString(), anyString(), any(), any())).thenReturn(supplier);

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new SupplierDto.CreateRequest("PROV-TEST", "Proveedor Test", "555-0000"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("PROV-TEST"))
                .andExpect(jsonPath("$.data.name").value("Proveedor Test"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_proveedor_sin_nombre_retorna_400() throws Exception {
        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"PROV-001\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_proveedor_duplicado_retorna_409() throws Exception {
        when(supplierService.create(anyString(), anyString(), any(), any()))
                .thenThrow(new ConflictException("Ya existe un proveedor con la clave: PROV-001"));

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new SupplierDto.CreateRequest("PROV-001", "Nombre", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un proveedor con la clave: PROV-001"));
    }

    @Test
    void crear_proveedor_sin_autenticacion_retorna_401() throws Exception {
        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new SupplierDto.CreateRequest("PROV-NEW", "Nuevo", null))))
                .andExpect(status().isUnauthorized());
    }
}
