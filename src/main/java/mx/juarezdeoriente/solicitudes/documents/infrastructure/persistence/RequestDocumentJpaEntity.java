package mx.juarezdeoriente.solicitudes.documents.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_documents")
public class RequestDocumentJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "request_id", nullable = false, columnDefinition = "uuid")
    private UUID requestId;

    @Column(name = "template_version", nullable = false, length = 30)
    private String templateVersion;

    @Column(name = "file_size_bytes")
    private Integer fileSizeBytes;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @Column(name = "generated_by", columnDefinition = "uuid")
    private UUID generatedBy;

    public RequestDocumentJpaEntity() {}

    public UUID getId()                      { return id; }
    public void setId(UUID v)                { this.id = v; }
    public UUID getRequestId()               { return requestId; }
    public void setRequestId(UUID v)         { this.requestId = v; }
    public String getTemplateVersion()       { return templateVersion; }
    public void setTemplateVersion(String v) { this.templateVersion = v; }
    public Integer getFileSizeBytes()        { return fileSizeBytes; }
    public void setFileSizeBytes(Integer v)  { this.fileSizeBytes = v; }
    public String getChecksum()              { return checksum; }
    public void setChecksum(String v)        { this.checksum = v; }
    public Instant getGeneratedAt()          { return generatedAt; }
    public void setGeneratedAt(Instant v)    { this.generatedAt = v; }
    public UUID getGeneratedBy()             { return generatedBy; }
    public void setGeneratedBy(UUID v)       { this.generatedBy = v; }
}
