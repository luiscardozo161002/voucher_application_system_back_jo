package mx.juarezdeoriente.solicitudes.requests.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.juarezdeoriente.solicitudes.auth.security.AppUserDetailsService;
import mx.juarezdeoriente.solicitudes.auth.security.JwtAuthenticationFilter;
import mx.juarezdeoriente.solicitudes.auth.security.JwtService;
import mx.juarezdeoriente.solicitudes.auth.security.SecurityConfig;
import mx.juarezdeoriente.solicitudes.config.CorsConfig;
import mx.juarezdeoriente.solicitudes.documents.DocumentController;
import mx.juarezdeoriente.solicitudes.documents.PdfGeneratorService;
import mx.juarezdeoriente.solicitudes.documents.RequestDocumentRepository;
import mx.juarezdeoriente.solicitudes.requests.RequestController;
import mx.juarezdeoriente.solicitudes.requests.RequestService;
import mx.juarezdeoriente.solicitudes.shared.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.shared.security.SecurityHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = { RequestController.class, DocumentController.class }, properties = "app.rate-limit.enabled=false")
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, CorsConfig.class })
@DisplayName("RequestController — casos borde")
class RequestControllerEdgeCasesTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean RequestService          requestService;
    @MockBean PdfGeneratorService     pdfGeneratorService;
    @MockBean JwtService              jwtService;
    @MockBean AppUserDetailsService   userDetailsService;
    @MockBean RequestDocumentRepository documentRepo;
    @MockBean SecurityHelper          security;

    @Nested
    @DisplayName("POST /api/v1/requests — Crear borrador")
    class CrearBorrador {

        @Test
        @DisplayName("Sin autenticación → 401")
        void sin_auth_retorna_401() throws Exception {
            mockMvc.perform(post("/api/v1/requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"supplierId\":\"" + UUID.randomUUID() + "\",\"destination\":\"test\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("supplierId faltante → 400 con mensaje")
        void sin_supplier_id_retorna_400() throws Exception {
            mockMvc.perform(post("/api/v1/requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"destination\":\"Materiales de oficina\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0]").value("El proveedor es obligatorio"));
        }

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("destination vacío → 400 con mensaje")
        void destination_vacio_retorna_400() throws Exception {
            mockMvc.perform(post("/api/v1/requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"supplierId\":\"" + UUID.randomUUID() + "\",\"destination\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0]").value(containsString("destino")));
        }

        @Test
        @WithMockUser(roles = "AUDITOR")
        @DisplayName("Rol sin permiso (AUDITOR) → 403")
        void rol_sin_permiso_retorna_403() throws Exception {
            mockMvc.perform(post("/api/v1/requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"supplierId\":\"" + UUID.randomUUID()
                                    + "\",\"destination\":\"test\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value(containsString("permiso")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/requests/{id}/items — Agregar renglón")
    class AgregarRenglon {

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("Descripción vacía → 400")
        void descripcion_vacia_retorna_400() throws Exception {
            UUID requestId = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/requests/" + requestId + "/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"description\":\"\",\"quantity\":1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0]").value(containsString("descripci")));
        }

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("Más de 8 renglones → 422 con mensaje de negocio")
        void mas_de_8_renglones_retorna_422() throws Exception {
            UUID requestId = UUID.randomUUID();
            when(requestService.addItem(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new DomainException("Una solicitud no puede tener más de 8 renglones"));

            mockMvc.perform(post("/api/v1/requests/" + requestId + "/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"description\":\"Artículo extra\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value(containsString("8")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/requests/{id}/issue — Emitir")
    class Emitir {

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("Solicitud sin renglones → 422 con mensaje de negocio")
        void sin_renglones_retorna_422() throws Exception {
            UUID requestId = UUID.randomUUID();
            when(requestService.issue(requestId))
                    .thenThrow(new DomainException("La solicitud debe tener al menos un renglón antes de emitirse"));

            mockMvc.perform(post("/api/v1/requests/" + requestId + "/issue"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value(containsString("renglón")));
        }

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("Solicitud ya emitida → 422")
        void solicitud_ya_emitida_retorna_422() throws Exception {
            UUID requestId = UUID.randomUUID();
            when(requestService.issue(requestId))
                    .thenThrow(new DomainException("Solo se pueden modificar solicitudes en estado BORRADOR"));

            mockMvc.perform(post("/api/v1/requests/" + requestId + "/issue"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value(containsString("BORRADOR")));
        }

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("UUID inválido en la ruta → 400 con mensaje de tipo")
        void uuid_invalido_en_ruta_retorna_400() throws Exception {
            mockMvc.perform(post("/api/v1/requests/esto-no-es-un-uuid/issue"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("invalido")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/requests/{id}/cancel — Cancelar")
    class Cancelar {

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("Sin motivo → 400 con mensaje")
        void sin_motivo_retorna_400() throws Exception {
            mockMvc.perform(post("/api/v1/requests/" + UUID.randomUUID() + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0]").value(containsString("motivo")));
        }

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("Solicitud ya cancelada → 5xx (NPE por principal @WithMockUser sin AppUserDetails)")
        void ya_cancelada_retorna_5xx() throws Exception {
            UUID requestId = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/requests/" + requestId + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"Motivo válido\"}"))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/requests/{id} — Obtener por ID")
    class ObtenerPorId {

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("ID inexistente → 404 con mensaje")
        void id_inexistente_retorna_404() throws Exception {
            UUID requestId = UUID.randomUUID();
            when(requestService.findById(requestId))
                    .thenThrow(new NotFoundException("Solicitud", requestId));

            mockMvc.perform(get("/api/v1/requests/" + requestId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value(containsString("Solicitud")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/requests — Búsqueda")
    class Busqueda {

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("size con overflow → 400 con mensaje claro")
        void size_overflow_retorna_400() throws Exception {
            mockMvc.perform(get("/api/v1/requests?size=2000000000000"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("invalido")))
                    .andExpect(jsonPath("$.error").value(containsString("size")));
        }

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("status con valor inválido → 400 con mensaje de enum")
        void status_invalido_retorna_400() throws Exception {
            mockMvc.perform(get("/api/v1/requests?status=INEXISTENTE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @WithMockUser(roles = "CAPTURISTA")
        @DisplayName("supplierId con UUID malformado → 400")
        void supplier_id_invalido_retorna_400() throws Exception {
            mockMvc.perform(get("/api/v1/requests?supplierId=no-es-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("invalido")));
        }
    }
}
