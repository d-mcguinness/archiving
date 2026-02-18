package com.dmc.archiving;

import com.dmc.archiving.storage.CloudStorageService;
import com.dmc.archiving.storage.StorageException;
import com.dmc.archiving.storage.UploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:4173"})
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);
    private final CloudStorageService cloudStorageService;

    public FileUploadController(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Upload to cloud storage
            UploadResult result = cloudStorageService.uploadFile(file, null);

            log.info("File uploaded successfully to cloud: {} (original: {})",
                    result.getFileKey(), result.getOriginalFilename());

            // Prepare success response
            response.put("success", true);
            response.put("message", "File uploaded successfully to cloud storage!");
            response.put("fileKey", result.getFileKey());
            response.put("fileUrl", result.getFileUrl());
            response.put("originalFilename", result.getOriginalFilename());
            response.put("size", result.getFileSize());
            response.put("contentType", result.getContentType());
            response.put("uploadTime", result.getUploadTime());

            return ResponseEntity.ok(response);

        } catch (StorageException e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/upload/user")
    public ResponseEntity<?> uploadFileForUser(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate userId
            if (userId == null || userId <= 0) {
                response.put("success", false);
                response.put("message", "Invalid user ID");
                return ResponseEntity.badRequest().body(response);
            }

            // Upload to cloud storage with user context
            UploadResult result = cloudStorageService.uploadFile(file, userId);

            log.info("File uploaded successfully to cloud for user {}: {} (original: {})",
                    userId, result.getFileKey(), result.getOriginalFilename());

            // Prepare success response
            response.put("success", true);
            response.put("message", "File uploaded successfully to cloud storage!");
            response.put("fileKey", result.getFileKey());
            response.put("fileUrl", result.getFileUrl());
            response.put("originalFilename", result.getOriginalFilename());
            response.put("userId", userId);
            response.put("size", result.getFileSize());
            response.put("contentType", result.getContentType());
            response.put("uploadTime", result.getUploadTime());

            return ResponseEntity.ok(response);

        } catch (StorageException e) {
            log.error("Failed to upload file for user {}: {}", userId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during file upload for user {}: {}", userId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Download file from cloud storage
     */
    @GetMapping("/download/{fileKey}")
    public ResponseEntity<?> downloadFile(@PathVariable String fileKey) {
        try {
            String presignedUrl = cloudStorageService.getPresignedUrl(fileKey, 60); // 1 hour expiry

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("downloadUrl", presignedUrl);
            response.put("message", "Presigned URL generated successfully");

            return ResponseEntity.ok(response);

        } catch (StorageException e) {
            log.error("Failed to generate download URL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to generate download URL"));
        }
    }

    /**
     * Get storage info endpoint
     */
    @GetMapping("/upload/info")
    public ResponseEntity<?> getUploadInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("storageType", "AWS S3 Cloud Storage");
        info.put("maxFileSize", 50 * 1024 * 1024);
        info.put("maxFileSizeMB", 50);
        info.put("supportedOperations", new String[]{
            "upload", "download", "delete", "presigned-urls"
        });

        return ResponseEntity.ok(info);
    }
}

