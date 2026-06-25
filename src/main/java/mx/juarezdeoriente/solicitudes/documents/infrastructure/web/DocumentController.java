package mx.juarezdeoriente.solicitudes.documents.infrastructure.web;

import mx.juarezdeoriente.solicitudes.documents.application.service.PdfGeneratorService;
import mx.juarezdeoriente.solicitudes.documents.infrastructure.persistence.RequestDocumentJpaEntity;
import mx.juarezdeoriente.solicitudes.documents.infrastructure.persistence.RequestDocumentJpaRepository;
import mx.juarezdeoriente.solicitudes.shared.infrastructure.web.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
public class DocumentController {

    private final PdfGeneratorService         pdfGeneratorService;
    private final RequestDocumentJpaRepository documentRepository;

    public DocumentController(PdfGeneratorService pdfGeneratorService,
                              RequestDocumentJpaRepository documentRepository) {
        this.pdfGeneratorService = pdfGeneratorService;
        this.documentRepository  = documentRepository;
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPdf(@PathVariable UUID id) {
        byte[] pdf = pdfGeneratorService.generateForRequest(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"solicitud-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Historial de PDFs generados para la solicitud: versión de plantilla, tamaño y checksum. */
    @GetMapping("/{id}/documents")
    public ResponseEntity<ApiResponse<List<DocumentSummary>>> getDocuments(@PathVariable UUID id) {
        List<DocumentSummary> history = documentRepository
                .findByRequestIdOrderByGeneratedAtDesc(id)
                .stream()
                .map(DocumentSummary::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    record DocumentSummary(
            UUID id, String templateVersion, Integer fileSizeBytes,
            String checksum, Instant generatedAt
    ) {
        static DocumentSummary from(RequestDocumentJpaEntity e) {
            return new DocumentSummary(e.getId(), e.getTemplateVersion(),
                    e.getFileSizeBytes(), e.getChecksum(), e.getGeneratedAt());
        }
    }
}
