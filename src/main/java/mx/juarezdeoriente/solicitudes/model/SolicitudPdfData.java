package mx.juarezdeoriente.solicitudes.model;
import mx.juarezdeoriente.solicitudes.model.RequestItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos aplanados que necesita la plantilla Thymeleaf para el PDF.
 * No depende de JPA ni de ningún agregado de dominio.
 */
public record SolicitudPdfData(
        String folioFormatted,
        LocalDateTime fechaEmision,
        String proveedor,
        String proveedorTelefono,
        String destino,
        String autorizador,
        String solicitante,
        List<ItemData> items,
        BigDecimal totalGeneral
) {
    public record ItemData(
            int posicion,
            String trabajador,
            String tipoTrabajador,
            String descripcion,
            BigDecimal cantidad,
            String unidad,
            BigDecimal costoUnitario,
            BigDecimal total
    ) {}
}
