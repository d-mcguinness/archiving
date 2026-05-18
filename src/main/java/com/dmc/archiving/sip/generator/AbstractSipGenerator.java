package com.dmc.archiving.sip.generator;

import com.dmc.archiving.sip.input.FileMetadataInput;
import com.dmc.archiving.sip.model.Sip;
import com.dmc.archiving.storage.CloudStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractSipGenerator implements SipGenerator {

    private static final Logger log = LoggerFactory.getLogger(AbstractSipGenerator.class);

    protected final CloudStorageService cloudStorageService;
    protected final ObjectMapper objectMapper;

    protected AbstractSipGenerator(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String generate(Sip sip) {
        try {
            SipSnapshot snapshot = SipSnapshot.from(sip);
            Map<String, Object> sipPackage = buildPackage(snapshot);
            byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(sipPackage);
            String fileKey = "sips/" + sip.getId() + "/" + getStandardName().toLowerCase() + "_sip.json";
            return cloudStorageService.uploadBytes(jsonBytes, fileKey, "application/json");
        } catch (Exception e) {
            log.error("Failed to generate SIP package for SIP {}: {}", sip.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate SIP package", e);
        }
    }

    protected record PrefillContext(
            String id,
            String name,
            String date,
            String user,
            String size,
            String type,
            int count,
            String checksum,
            Long fileSizeBytes) {}

    protected PrefillContext prefillContext(FileMetadataInput meta) {
        return new PrefillContext(
                generateId(),
                stripExtension(meta.getFilename()),
                meta.getUploadedAt() != null ? meta.getUploadedAt() : today(),
                meta.getUploaderName() != null ? meta.getUploaderName() : "System",
                meta.getFileSize() != null ? meta.getFileSize().toString() : "0",
                meta.getContentType() != null ? meta.getContentType() : "application/octet-stream",
                meta.getFileCount() > 0 ? meta.getFileCount() : 1,
                meta.getChecksum(),
                meta.getFileSize()
        );
    }

    protected static String generateId() {
        return "SIP-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected static String stripExtension(String filename) {
        if (filename == null) return "Untitled";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    protected static String today() {
        return LocalDate.now().toString();
    }

    protected static String mapContentType(String contentType) {
        if (contentType == null) return "Digital";
        if (contentType.startsWith("image/")) return "Image";
        if (contentType.startsWith("video/")) return "Video";
        if (contentType.startsWith("audio/")) return "Audio";
        if (contentType.startsWith("text/")) return "Text";
        if (contentType.contains("pdf")) return "Document";
        if (contentType.contains("xml")) return "Structured Data";
        if (contentType.contains("json")) return "Structured Data";
        if (contentType.contains("spreadsheet") || contentType.contains("excel")) return "Dataset";
        return "Digital";
    }

    protected static String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
