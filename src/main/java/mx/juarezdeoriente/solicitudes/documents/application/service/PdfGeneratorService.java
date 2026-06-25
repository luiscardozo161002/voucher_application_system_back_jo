package mx.juarezdeoriente.solicitudes.documents.application.service;

import mx.juarezdeoriente.solicitudes.config.CompanyProperties;
import mx.juarezdeoriente.solicitudes.documents.domain.model.SolicitudPdfData;
import mx.juarezdeoriente.solicitudes.documents.infrastructure.persistence.RequestDocumentJpaEntity;
import mx.juarezdeoriente.solicitudes.documents.infrastructure.persistence.RequestDocumentJpaRepository;
import mx.juarezdeoriente.solicitudes.requests.application.service.RequestService;
import mx.juarezdeoriente.solicitudes.requests.domain.model.Request;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestStatus;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import mx.juarezdeoriente.solicitudes.suppliers.application.service.SupplierService;
import mx.juarezdeoriente.solicitudes.suppliers.domain.model.Supplier;
import mx.juarezdeoriente.solicitudes.workers.application.service.WorkerService;
import mx.juarezdeoriente.solicitudes.workers.domain.model.Worker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class PdfGeneratorService {

    /** Versión semántica de la plantilla. Incrementar al modificar el HTML del PDF. */
    static final String TEMPLATE_VERSION = "1.0.0";

    private final RequestService               requestService;
    private final SupplierService              supplierService;
    private final WorkerService                workerService;
    private final TemplateEngine               templateEngine;
    private final RequestDocumentJpaRepository documentRepository;
    private final CompanyProperties            company;

    public PdfGeneratorService(RequestService requestService,
                               SupplierService supplierService,
                               WorkerService workerService,
                               TemplateEngine templateEngine,
                               RequestDocumentJpaRepository documentRepository,
                               CompanyProperties company) {
        this.requestService     = requestService;
        this.supplierService    = supplierService;
        this.workerService      = workerService;
        this.templateEngine     = templateEngine;
        this.documentRepository = documentRepository;
        this.company            = company;
    }

    @Transactional
    public byte[] generateForRequest(UUID requestId) {
        Request request = requestService.findById(requestId);

        if (request.getStatus() == RequestStatus.BORRADOR) {
            throw new DomainException("No se puede generar un PDF de una solicitud en estado BORRADOR");
        }

        Supplier supplier = supplierService.findById(request.getSupplierId());
        List<SolicitudPdfData.ItemData> items = buildItems(request);

        BigDecimal totalGeneral = items.stream()
                .map(SolicitudPdfData.ItemData::total)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ZoneId mx = ZoneId.of("America/Mexico_City");
        var issuedAt = request.getIssuedAt() != null
                ? ZonedDateTime.ofInstant(request.getIssuedAt(), mx).toLocalDateTime()
                : ZonedDateTime.now(mx).toLocalDateTime();

        SolicitudPdfData data = new SolicitudPdfData(
                String.format("%07d", request.getFolio()),
                issuedAt,
                supplier.getName(),
                supplier.getPhone(),
                request.getDestination(),
                request.getAuthorizer(),
                null,
                items,
                totalGeneral
        );

        byte[] pdfBytes = renderPdf(data);

        // Registrar documento generado con versión de plantilla y checksum
        recordDocument(requestId, pdfBytes);

        return pdfBytes;
    }

    private List<SolicitudPdfData.ItemData> buildItems(Request request) {
        return request.getItems().stream().map(item -> {
            String workerName = null;
            String workerType = null;
            if (item.getWorkerId() != null) {
                try {
                    Worker w = workerService.findById(item.getWorkerId());
                    workerName = w.getName();
                    workerType = w.getWorkerType().name();
                } catch (Exception ignored) {}
            }
            return new SolicitudPdfData.ItemData(
                    item.getPosition(), workerName, workerType,
                    item.getDescription(), item.getQuantity(),
                    item.getUnit(), item.getUnitCost(), item.getTotal()
            );
        }).toList();
    }

    private byte[] renderPdf(SolicitudPdfData data) {
        Context ctx = new Context();
        ctx.setVariable("solicitud", data);
        ctx.setVariable("empresa", company);
        String xhtml = templateEngine.process("pdf/solicitud", ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error generando PDF para la solicitud", ex);
        }
    }

    private void recordDocument(UUID requestId, byte[] pdfBytes) {
        try {
            String checksum = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(pdfBytes));

            RequestDocumentJpaEntity doc = new RequestDocumentJpaEntity();
            doc.setId(UUID.randomUUID());
            doc.setRequestId(requestId);
            doc.setTemplateVersion(TEMPLATE_VERSION);
            doc.setFileSizeBytes(pdfBytes.length);
            doc.setChecksum(checksum);
            doc.setGeneratedAt(Instant.now());
            documentRepository.save(doc);
        } catch (NoSuchAlgorithmException ex) {
            // No bloquear la generación del PDF si el registro falla
        }
    }
}
