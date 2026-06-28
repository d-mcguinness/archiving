package com.dmc.archiving.intake.generator;

import com.dmc.archiving.pkg.generator.AbstractPackageGenerator;
import com.dmc.archiving.intake.input.FileMetadataInput;
import com.dmc.archiving.intake.model.Intake;
import com.dmc.archiving.storage.CloudStorageService;

import java.time.LocalDate;
import java.util.UUID;

public abstract class AbstractIntakeGenerator
        extends AbstractPackageGenerator<Intake, IntakeSnapshot>
        implements IntakeGenerator {

    protected AbstractIntakeGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    protected String packageType() {
        return "sip";
    }

    @Override
    protected Long packageId(Intake sip) {
        return sip.getId();
    }

    @Override
    protected IntakeSnapshot toSnapshot(Intake sip) {
        return IntakeSnapshot.from(sip);
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
