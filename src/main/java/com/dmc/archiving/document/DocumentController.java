package com.dmc.archiving.document;

import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.document.model.DocumentStatus;
import com.dmc.archiving.storage.CloudStorageService;
import com.dmc.archiving.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Document operations
 * Provides role-based access to documents:
 * - ADMIN: See all documents
 * - TENANT: See all documents in their tenant
 * - USER: See their own documents
 */
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:4173", "http://localhost:5173"})
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final CloudStorageService cloudStorageService;

    public DocumentController(DocumentService documentService, CloudStorageService cloudStorageService) {
        this.documentService = documentService;
        this.cloudStorageService = cloudStorageService;
    }

    /**
     * Upload a new document
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {

        try {
            log.info("Uploading document for user {}, tenant {}", userId, tenantId);

            if (file.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "File is empty"));
            }

            Document document = documentService.uploadDocument(file, userId, tenantId, title, description);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document uploaded successfully");
            response.put("document", convertToMap(document));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading document: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to upload document: " + e.getMessage()));
        }
    }

    /**
     * Get all documents (filtered by role)
     * Query params: role, userId, tenantId
     */
    @GetMapping
    public ResponseEntity<?> getDocuments(
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "sipId", required = false) Long sipId) {

        try {
            List<Document> documents;

            // SIP-specific filtering
            if (sipId != null) {
                documents = documentService.getDocumentsBySip(sipId);
                log.info("Fetching documents for SIP {}", sipId);
            } else if ("ADMIN".equals(role)) {
                // Admin sees all documents
                documents = documentService.getAllDocuments();
                log.info("Fetching all documents for ADMIN");
            } else if ("TENANT".equals(role)) {
                if (tenantId != null) {
                    documents = documentService.getDocumentsByTenant(tenantId);
                    log.info("Fetching documents for tenant {}", tenantId);
                } else if (userId != null) {
                    documents = documentService.getDocumentsByUserTenants(userId);
                    log.info("Fetching documents for TENANT user {} from their tenant(s)", userId);
                } else {
                    return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "error", "Missing userId or tenantId for TENANT role"));
                }
            } else if ("USER".equals(role) && userId != null) {
                if (tenantId != null) {
                    documents = documentService.getDocumentsByUserAndTenant(userId, tenantId);
                    log.info("Fetching documents for USER {} in tenant {}", userId, tenantId);
                } else {
                    documents = documentService.getDocumentsByUser(userId);
                    log.info("Fetching documents for USER {}", userId);
                }
            } else if (userId != null) {
                // Default: user sees their own documents
                documents = documentService.getDocumentsByUser(userId);
                log.info("Fetching documents for user {}", userId);
            } else {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Missing required parameters"));
            }

            List<Map<String, Object>> documentMaps = documents.stream()
                .map(this::convertToMap)
                .toList();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "documents", documentMaps,
                "count", documentMaps.size()
            ));

        } catch (Exception e) {
            log.error("Error fetching documents: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to fetch documents: " + e.getMessage()));
        }
    }

    /**
     * Get document by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable Long id) {
        try {
            return documentService.getDocumentById(id)
                .map(document -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "document", convertToMap(document)
                )))
                .orElse(ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Document not found")));
        } catch (Exception e) {
            log.error("Error fetching document {}: {}", id, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to fetch document"));
        }
    }

    /**
     * Update document
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocument(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        try {
            String title = (String) updates.get("title");
            String description = (String) updates.get("description");
            DocumentStatus status = updates.get("status") != null
                ? DocumentStatus.valueOf((String) updates.get("status"))
                : null;

            Document document = documentService.updateDocument(id, title, description, status);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document updated successfully",
                "document", convertToMap(document)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating document {}: {}", id, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to update document"));
        }
    }

    /**
     * Delete document
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            boolean deleted = documentService.deleteDocument(id);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Document deleted successfully"
                ));
            } else {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Document not found"));
            }

        } catch (Exception e) {
            log.error("Error deleting document {}: {}", id, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to delete document"));
        }
    }

    /**
     * Get download URL for document
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> getDownloadUrl(@PathVariable Long id) {
        try {
            String downloadUrl = documentService.getDownloadUrl(id);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "downloadUrl", downloadUrl,
                "expiresIn", "1 hour"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating download URL for document {}: {}", id, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to generate download URL"));
        }
    }

    /**
     * Stream document file directly to the browser
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<?> downloadFile(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

            InputStream inputStream = cloudStorageService.downloadFile(document.getFileKey());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", document.getFileName());
            if (document.getFileSize() != null) {
                headers.setContentLength(document.getFileSize());
            }

            MediaType mediaType = document.getContentType() != null
                ? MediaType.parseMediaType(document.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new InputStreamResource(inputStream));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (StorageException e) {
            log.warn("File not found in storage for document {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "error", "File not found in storage. It may have been deleted or the storage was reset."));
        } catch (Exception e) {
            log.error("Error downloading document {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to download document"));
        }
    }

    /**
     * Associate document with archive
     */
    @PostMapping("/{id}/associate-archive")
    public ResponseEntity<?> associateWithArchive(
            @PathVariable Long id,
            @RequestParam Long archiveId) {

        try {
            Document document = documentService.associateWithArchive(id, archiveId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document associated with archive successfully",
                "document", convertToMap(document)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error associating document {} with archive: {}", id, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to associate document with archive"));
        }
    }

    /**
     * Associate document with SIP
     */
    @PostMapping("/{id}/associate-sip")
    public ResponseEntity<?> associateWithSip(
            @PathVariable Long id,
            @RequestParam Long sipId) {

        try {
            Document document = documentService.associateWithSip(id, sipId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document associated with SIP successfully",
                "document", convertToMap(document)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error associating document {} with SIP: {}", id, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to associate document with SIP"));
        }
    }

    /**
     * Disassociate document from SIP
     */
    @PostMapping("/{id}/disassociate-sip")
    public ResponseEntity<?> disassociateFromSip(@PathVariable Long id) {
        try {
            Document document = documentService.disassociateFromSip(id);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document disassociated from SIP successfully",
                "document", convertToMap(document)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error disassociating document {} from SIP: {}", id, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to disassociate document from SIP"));
        }
    }

    /**
     * Convert Document entity to Map for JSON response
     */
    private Map<String, Object> convertToMap(Document document) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", document.getId());
        map.put("title", document.getTitle());
        map.put("description", document.getDescription());
        map.put("fileName", document.getFileName());
        map.put("fileSize", document.getFileSize());
        map.put("contentType", document.getContentType());
        map.put("userId", document.getUserId());
        map.put("tenantId", document.getTenantId());
        map.put("archiveId", document.getArchiveId());
        map.put("sipId", document.getSipId());
        map.put("status", document.getStatus().name());
        map.put("createdAt", document.getCreatedAt().toString());
        map.put("updatedAt", document.getUpdatedAt() != null ? document.getUpdatedAt().toString() : null);
        map.put("uploadedAt", document.getUploadedAt() != null ? document.getUploadedAt().toString() : null);
        return map;
    }
}

