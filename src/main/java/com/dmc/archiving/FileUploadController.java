package com.dmc.archiving;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:4173"})
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);
    private static final String UPLOAD_DIR = "uploads/";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                response.put("success", false);
                response.put("message", "File size exceeds maximum limit of 50MB");
                return ResponseEntity.badRequest().body(response);
            }

            // Get original filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                response.put("success", false);
                response.put("message", "Invalid filename");
                return ResponseEntity.badRequest().body(response);
            }

            // Sanitize filename
            String sanitizedFilename = sanitizeFilename(originalFilename);

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueFilename = timestamp + "_" + sanitizedFilename;
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File uploaded successfully: {} (original: {})", uniqueFilename, originalFilename);

            // Prepare success response
            response.put("success", true);
            response.put("message", "File uploaded successfully!");
            response.put("filename", uniqueFilename);
            response.put("originalFilename", originalFilename);
            response.put("size", file.getSize());
            response.put("contentType", file.getContentType());
            response.put("uploadTime", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
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
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                response.put("success", false);
                response.put("message", "File size exceeds maximum limit of 50MB");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate userId
            if (userId == null || userId <= 0) {
                response.put("success", false);
                response.put("message", "Invalid user ID");
                return ResponseEntity.badRequest().body(response);
            }

            // Get original filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                response.put("success", false);
                response.put("message", "Invalid filename");
                return ResponseEntity.badRequest().body(response);
            }

            // Sanitize filename
            String sanitizedFilename = sanitizeFilename(originalFilename);

            // Create user-specific upload directory
            Path userUploadPath = Paths.get(UPLOAD_DIR, "users", String.valueOf(userId));
            if (!Files.exists(userUploadPath)) {
                Files.createDirectories(userUploadPath);
            }

            // Generate unique filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueFilename = timestamp + "_" + sanitizedFilename;
            Path filePath = userUploadPath.resolve(uniqueFilename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File uploaded successfully for user {}: {} (original: {})",
                    userId, uniqueFilename, originalFilename);

            // Prepare success response
            response.put("success", true);
            response.put("message", "File uploaded successfully!");
            response.put("filename", uniqueFilename);
            response.put("originalFilename", originalFilename);
            response.put("userId", userId);
            response.put("size", file.getSize());
            response.put("contentType", file.getContentType());
            response.put("uploadTime", LocalDateTime.now().toString());
            response.put("filePath", "uploads/users/" + userId + "/" + uniqueFilename);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
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
     * Sanitize filename to prevent directory traversal and other security issues
     */
    private String sanitizeFilename(String filename) {
        // Remove path separators and null bytes
        // Use \\x00 instead of \\0 to avoid illegal octal escape sequence
        String sanitized = filename.replaceAll("[/\\\\\\x00]", "_");

        // Remove leading dots to prevent hidden files
        sanitized = sanitized.replaceAll("^\\.+", "");

        // If filename becomes empty after sanitization, use a default
        if (sanitized.isEmpty()) {
            sanitized = "uploaded_file";
        }

        return sanitized;
    }

    /**
     * Get file info endpoint (optional - for checking uploaded files)
     */
    @GetMapping("/upload/info")
    public ResponseEntity<?> getUploadInfo() {
        Map<String, Object> info = new HashMap<>();

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);

            info.put("uploadDirectory", uploadPath.toAbsolutePath().toString());
            info.put("maxFileSize", MAX_FILE_SIZE);
            info.put("maxFileSizeMB", MAX_FILE_SIZE / (1024 * 1024));
            info.put("directoryExists", Files.exists(uploadPath));

            if (Files.exists(uploadPath)) {
                long fileCount = Files.list(uploadPath).count();
                info.put("fileCount", fileCount);
            }

            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Failed to get upload info: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get upload info"));
        }
    }
}
