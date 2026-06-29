package com.tacitiq.modules.document.controller;

import com.tacitiq.modules.auth.dto.CustomUserDetails;
import com.tacitiq.modules.document.dto.DocumentDto;
import com.tacitiq.modules.document.service.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentDto>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> getDocumentById(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER', 'MAINTENANCE_ENGINEER', 'RELIABILITY_ENGINEER', 'HSE_OFFICER')")
    public ResponseEntity<DocumentDto> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("docType") String docType,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {
        
        UUID userId = userDetails != null ? userDetails.getId() : null;
        DocumentDto uploaded = documentService.uploadDocument(file, docType, userId);
        return ResponseEntity.ok(uploaded);
    }
}
