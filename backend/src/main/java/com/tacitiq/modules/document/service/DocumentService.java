package com.tacitiq.modules.document.service;

import com.tacitiq.modules.document.dto.DocumentDto;
import com.tacitiq.modules.document.entity.Document;
import com.tacitiq.modules.document.repository.DocumentRepository;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Tika tika = new Tika();
    private final Path uploadPath;

    public DocumentService(
            DocumentRepository documentRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${tacitiq.ai.upload-dir}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.eventPublisher = eventPublisher;
        this.uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            log.error("Failed to create upload directories", e);
        }
    }

    public List<DocumentDto> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public DocumentDto getDocumentById(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + id));
        return mapToDto(doc);
    }

    public DocumentDto uploadDocument(MultipartFile file, String docType, UUID userId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        // MIME validation via Tika (prevent script uploads masquerading as PDFs)
        String mimeType;
        try {
            mimeType = tika.detect(file.getInputStream());
            log.info("Detected MIME type for upload '{}': {}", originalFilename, mimeType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to analyze file integrity", e);
        }

        // Validate allowed MIME types
        List<String> allowedTypes = List.of(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "image/png",
                "image/jpeg",
                "image/tiff",
                "text/plain"
        );
        if (!allowedTypes.contains(mimeType)) {
            throw new IllegalArgumentException("File type not supported: " + mimeType);
        }

        // Sanitize file path
        String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".txt";
        String secureFilename = UUID.randomUUID().toString() + extension;
        Path targetPath = uploadPath.resolve(secureFilename);

        // Save file physically
        Files.copy(file.getInputStream(), targetPath);

        Document doc = Document.builder()
                .docType(docType)
                .title(originalFilename)
                .storagePath(targetPath.toAbsolutePath().toString())
                .relatedAssets(new ArrayList<>())
                .version(1)
                .embeddingStatus("PENDING")
                .chunkCount(0)
                .uploadedBy(userId)
                .build();

        Document saved = documentRepository.save(doc);
        DocumentDto dto = mapToDto(saved);

        // Publish Spring event to trigger processing asynchronously
        eventPublisher.publishEvent(dto);

        return dto;
    }

    @Transactional
    public void updateStatus(UUID id, String status, int chunkCount) {
        Document doc = documentRepository.findById(id).orElse(null);
        if (doc != null) {
            doc.setEmbeddingStatus(status);
            doc.setChunkCount(chunkCount);
            doc.setProcessedAt(OffsetDateTime.now());
            documentRepository.save(doc);
        }
    }

    @Transactional
    public void updateStatusAndMetadata(UUID id, String status, int chunkCount,
                                        String tags, String failures, String procedures,
                                        String safety, String workOrders, String findings, List<UUID> relatedAssets) {
        Document doc = documentRepository.findById(id).orElse(null);
        if (doc != null) {
            doc.setEmbeddingStatus(status);
            doc.setChunkCount(chunkCount);
            doc.setProcessedAt(OffsetDateTime.now());
            doc.setExtractedTags(tags);
            doc.setExtractedFailureModes(failures);
            doc.setExtractedProcedures(procedures);
            doc.setExtractedSafetyReferences(safety);
            doc.setExtractedWorkOrders(workOrders);
            doc.setExtractedFindings(findings);
            if (relatedAssets != null) {
                doc.setRelatedAssets(relatedAssets);
            }
            documentRepository.save(doc);
        }
    }

    private DocumentDto mapToDto(Document entity) {
        return new DocumentDto(
                entity.getId(),
                entity.getDocType(),
                entity.getTitle(),
                entity.getStoragePath(),
                entity.getRelatedAssets(),
                entity.getVersion(),
                entity.getEmbeddingStatus(),
                entity.getChunkCount(),
                entity.getUploadedBy(),
                entity.getProcessedAt(),
                entity.getExtractedTags(),
                entity.getExtractedFailureModes(),
                entity.getExtractedProcedures(),
                entity.getExtractedSafetyReferences(),
                entity.getExtractedWorkOrders(),
                entity.getExtractedFindings()
        );
    }
}
