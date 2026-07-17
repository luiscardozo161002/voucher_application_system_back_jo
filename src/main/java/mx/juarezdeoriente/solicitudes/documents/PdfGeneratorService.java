package mx.juarezdeoriente.solicitudes.documents;
import mx.juarezdeoriente.solicitudes.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.documents.RequestDocumentRepository;
import mx.juarezdeoriente.solicitudes.requests.RequestItem;
import mx.juarezdeoriente.solicitudes.documents.SolicitudPdfData;
import mx.juarezdeoriente.solicitudes.documents.RequestDocument;

import mx.juarezdeoriente.solicitudes.config.CompanyProperties;
import mx.juarezdeoriente.solicitudes.requests.Request;
import mx.juarezdeoriente.solicitudes.requests.RequestService;
import mx.juarezdeoriente.solicitudes.requests.RequestStatus;
import mx.juarezdeoriente.solicitudes.exception.DomainException;
import mx.juarezdeoriente.solicitudes.suppliers.Supplier;
import mx.juarezdeoriente.solicitudes.suppliers.SupplierService;
import mx.juarezdeoriente.solicitudes.workers.Worker;
import mx.juarezdeoriente.solicitudes.workers.WorkerService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
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

    private static final Logger log = LoggerFactory.getLogger(PdfGeneratorService.class);
    static final String TEMPLATE_VERSION = "1.0.0";

    private final ThreadLocal<ITextRenderer> rendererPool = ThreadLocal.withInitial(ITextRenderer::new);

    private String staticBaseUrl;

    private final RequestService           requestService;
    private final SupplierService          supplierService;
    private final WorkerService            workerService;
    private final TemplateEngine           templateEngine;
    private final RequestDocumentRepository documentRepository;
    private final CompanyProperties        company;

    public PdfGeneratorService(RequestService requestService,
                               SupplierService supplierService,
                               WorkerService workerService,
                               TemplateEngine templateEngine,
                               RequestDocumentRepository documentRepository,
                               CompanyProperties company) {
        this.requestService     = requestService;
        this.supplierService    = supplierService;
        this.workerService      = workerService;
        this.templateEngine     = templateEngine;
        this.documentRepository = documentRepository;
        this.company            = company;
    }

    @PostConstruct
    void warmUp() {
        try {
            staticBaseUrl = getClass().getResource("/static/").toExternalForm();
            ITextRenderer r = rendererPool.get();
            r.setDocumentFromString("<html><body></body></html>", staticBaseUrl);
            r.layout();
            log.info("PdfGeneratorService: renderer pre-calentado correctamente.");
        } catch (Exception e) {
            log.warn("PdfGeneratorService: pre-calentamiento falló — el primer PDF será más lento. {}", e.getMessage());
        }
    }

    @Transactional
    public byte[] generateForRequest(UUID requestId) {
        Request request = requestService.findById(requestId);

        if (request.getStatus() == RequestStatus.BORRADOR) {
            throw new DomainException("No se puede generar un PDF de una solicitud en estado BORRADOR");
        }

        Supplier supplier = supplierService.findById(request.getSupplierId());
        List<SolicitudPdfData.ItemData> items = buildItems(request);

        String solicitante = null;
        if (request.getSolicitanteId() != null) {
            try {
                solicitante = workerService.findById(request.getSolicitanteId()).getName();
            } catch (Exception ignored) {}
        }
        if (solicitante == null) {
            solicitante = items.stream()
                    .filter(i -> i.trabajador() != null)
                    .map(SolicitudPdfData.ItemData::trabajador)
                    .findFirst()
                    .orElse(null);
        }

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
                solicitante,
                items,
                totalGeneral
        );

        byte[] pdfBytes = renderPdf(data);
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
            ITextRenderer renderer = rendererPool.get();
            renderer.setDocumentFromString(xhtml, staticBaseUrl);
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

            RequestDocument doc = new RequestDocument();
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
