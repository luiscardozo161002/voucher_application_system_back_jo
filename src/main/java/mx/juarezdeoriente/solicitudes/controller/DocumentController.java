package mx.juarezdeoriente.solicitudes.controller;
import mx.juarezdeoriente.solicitudes.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.security.AppUserDetails;
import mx.juarezdeoriente.solicitudes.service.PdfGeneratorService;
import mx.juarezdeoriente.solicitudes.repository.RequestDocumentRepository;
import mx.juarezdeoriente.solicitudes.model.RequestDocument;

import mx.juarezdeoriente.solicitudes.shared.web.ApiResponse;
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

    private final PdfGeneratorService       pdfGeneratorService;
    private final RequestDocumentRepository documentRepository;

    public DocumentController(PdfGeneratorService pdfGeneratorService,
                              RequestDocumentRepository documentRepository) {
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
        static DocumentSummary from(RequestDocument e) {
            return new DocumentSummary(e.getId(), e.getTemplateVersion(),
                    e.getFileSizeBytes(), e.getChecksum(), e.getGeneratedAt());
        }
    }
}
