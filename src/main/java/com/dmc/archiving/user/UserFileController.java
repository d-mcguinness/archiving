package com.dmc.archiving.user;

import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for user file upload operations
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4173", "http://localhost:5173"})
public class UserFileController {

    private static final Logger log = LoggerFactory.getLogger(UserFileController.class);

    // Configure upload directory (can be moved to application.properties)
    private static final String UPLOAD_DIR = "uploads/users";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Autowired
    private UserService userService;

    /**
     * Upload a file for a specific user
     *
     * @param userId The user ID
     * @param file The file to upload
     * @return Response with upload status and file info
     */
    @PostMapping("/{userId}/upload")
    public ResponseEntity<?> uploadFile(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {

        try {
            log.info("Received file upload request for user {}: {}", userId, file.getOriginalFilename());

            // Validate user exists
            Optional<User> userOptional = userService.getUserById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "error", "User not found with ID: " + userId
                    ));
            }

            User user = userOptional.get();

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", "File is empty"
                    ));
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", "File size exceeds maximum limit of 10MB"
                    ));
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR, String.valueOf(userId));
            Files.createDirectories(uploadPath);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
            String uniqueFilename = UUID.randomUUID() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File uploaded successfully for user {}: {}", userId, uniqueFilename);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File uploaded successfully");

            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("originalName", originalFilename);
            fileInfo.put("storedName", uniqueFilename);
            fileInfo.put("filePath", filePath.toString());
            fileInfo.put("fileSize", file.getSize());
            fileInfo.put("contentType", file.getContentType());
            fileInfo.put("userId", userId);
            fileInfo.put("userName", user.getName());
            fileInfo.put("uploadedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            response.put("file", fileInfo);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to upload file for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to upload file: " + e.getMessage()
                ));
        } catch (Exception e) {
            log.error("Unexpected error uploading file for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "An unexpected error occurred: " + e.getMessage()
                ));
        }
    }

    /**
     * Get upload information/stats for a user
     *
     * @param userId The user ID
     * @return Upload statistics
     */
    @GetMapping("/{userId}/uploads")
    public ResponseEntity<?> getUserUploads(@PathVariable Long userId) {
        try {
            // Validate user exists
            Optional<User> userOptional = userService.getUserById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "error", "User not found with ID: " + userId
                    ));
            }

            User user = userOptional.get();

            Path uploadPath = Paths.get(UPLOAD_DIR, String.valueOf(userId));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("userName", user.getName());
            response.put("uploadPath", uploadPath.toString());
            response.put("pathExists", Files.exists(uploadPath));

            if (Files.exists(uploadPath)) {
                try (var stream = Files.list(uploadPath)) {
                    long fileCount = stream.count();
                    response.put("fileCount", fileCount);
                }
            } else {
                response.put("fileCount", 0);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get upload info for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to retrieve upload information: " + e.getMessage()
                ));
        }
    }

    /**
     * Delete a specific uploaded file
     *
     * @param userId The user ID
     * @param filename The filename to delete
     * @return Response with deletion status
     */
    @DeleteMapping("/{userId}/uploads/{filename}")
    public ResponseEntity<?> deleteUpload(
            @PathVariable Long userId,
            @PathVariable String filename) {

        try {
            // Validate user exists
            Optional<User> userOptional = userService.getUserById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "error", "User not found with ID: " + userId
                    ));
            }

            Path filePath = Paths.get(UPLOAD_DIR, String.valueOf(userId), filename);

            if (!Files.exists(filePath)) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "error", "File not found: " + filename
                    ));
            }

            Files.delete(filePath);

            log.info("File deleted successfully for user {}: {}", userId, filename);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "File deleted successfully",
                "filename", filename
            ));

        } catch (Exception e) {
            log.error("Failed to delete file for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to delete file: " + e.getMessage()
                ));
        }
    }
}
