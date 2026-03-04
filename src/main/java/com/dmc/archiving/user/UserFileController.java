package com.dmc.archiving.user;

import com.dmc.archiving.storage.CloudStorageService;
import com.dmc.archiving.storage.StorageException;
import com.dmc.archiving.storage.UploadResult;
import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for user file upload operations (Cloud Storage)
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:4173", "http://localhost:5173"})
public class UserFileController {

    private static final Logger log = LoggerFactory.getLogger(UserFileController.class);

    private final UserService userService;
    private final CloudStorageService cloudStorageService;

    public UserFileController(UserService userService, CloudStorageService cloudStorageService) {
        this.userService = userService;
        this.cloudStorageService = cloudStorageService;
    }

    /**
     * Upload a file for a specific user to cloud storage
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
            log.info("Received cloud file upload request for user {}: {}", userId, file.getOriginalFilename());

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

            // Upload to cloud storage
            UploadResult result = cloudStorageService.uploadFile(file, userId);

            log.info("File uploaded successfully to cloud for user {}: {}", userId, result.getFileKey());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File uploaded successfully to cloud storage");

            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("originalName", result.getOriginalFilename());
            fileInfo.put("fileKey", result.getFileKey());
            fileInfo.put("fileUrl", result.getFileUrl());
            fileInfo.put("fileSize", result.getFileSize());
            fileInfo.put("contentType", result.getContentType());
            fileInfo.put("userId", userId);
            fileInfo.put("userName", user.getName());
            fileInfo.put("uploadedAt", result.getUploadTime());

            response.put("file", fileInfo);

            return ResponseEntity.ok(response);

        } catch (StorageException e) {
            log.error("Failed to upload file to cloud for user {}: {}", userId, e.getMessage(), e);
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
     * Get presigned download URL for a user's file
     *
     * @param userId The user ID
     * @param fileKey The file key in cloud storage (URL encoded)
     * @return Presigned download URL
     */
    @GetMapping("/{userId}/download/{fileKey:.+}")
    public ResponseEntity<?> getDownloadUrl(
            @PathVariable Long userId,
            @PathVariable String fileKey) {
        try {
            log.info("Download URL request for user {}, file: {}", userId, fileKey);

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

            // Generate presigned URL (valid for 1 hour)
            String downloadUrl = cloudStorageService.getPresignedUrl(fileKey, 60);

            log.info("Presigned URL generated for user {}", userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "downloadUrl", downloadUrl,
                "message", "Download URL generated successfully",
                "expiresIn", "1 hour"
            ));

        } catch (StorageException e) {
            log.error("Failed to generate download URL for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to generate download URL: " + e.getMessage()
                ));
        } catch (Exception e) {
            log.error("Unexpected error generating download URL for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "An unexpected error occurred"
                ));
        }
    }

    /**
     * Delete a file from cloud storage
     *
     * @param userId The user ID
     * @param fileKey The file key to delete (URL encoded)
     * @return Response with deletion status
     */
    @DeleteMapping("/{userId}/uploads/{fileKey:.+}")
    public ResponseEntity<?> deleteUpload(
            @PathVariable Long userId,
            @PathVariable String fileKey) {

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

            // Check if file exists
            if (!cloudStorageService.fileExists(fileKey)) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "error", "File not found: " + fileKey
                    ));
            }

            // Delete file from cloud
            cloudStorageService.deleteFile(fileKey);

            log.info("File deleted successfully from cloud for user {}: {}", userId, fileKey);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "File deleted successfully",
                "fileKey", fileKey
            ));

        } catch (StorageException e) {
            log.error("Failed to delete file for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to delete file: " + e.getMessage()
                ));
        } catch (Exception e) {
            log.error("Unexpected error deleting file for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "An unexpected error occurred"
                ));
        }
    }
}

