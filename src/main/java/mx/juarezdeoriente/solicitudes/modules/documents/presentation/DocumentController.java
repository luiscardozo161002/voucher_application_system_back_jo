package mx.juarezdeoriente.solicitudes.modules.documents.presentation;
import mx.juarezdeoriente.solicitudes.modules.documents.infrastructure.RequestDocumentRepository;
import mx.juarezdeoriente.solicitudes.modules.documents.domain.RequestDocument;
import mx.juarezdeoriente.solicitudes.shared.web.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
public class DocumentController {

    private final RequestDocumentRepository documentRepository;

    public DocumentController(RequestDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
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
