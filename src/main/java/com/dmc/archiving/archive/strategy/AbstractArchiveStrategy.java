package com.dmc.archiving.archive.strategy;

import com.dmc.archiving.archive.model.Archive;

import java.util.HashMap;
import java.util.Map;

/**
 * Base implementation of ArchiveStrategy with common functionality
 */
public abstract class AbstractArchiveStrategy implements ArchiveStrategy {

    @Override
    public ValidationResult validate(Archive archive) {
        ValidationResult result = new ValidationResult();

        // Common validation
        if (archive.getTitle() == null || archive.getTitle().trim().isEmpty()) {
            result.addError("Archive title is required");
        }

        if (archive.getOwnerId() == null) {
            result.addError("Archive owner is required");
        }

        // Standard-specific validation
        validateStandard(archive, result);

        return result;
    }

    @Override
    public Map<String, Object> export(Archive archive) {
        Map<String, Object> exportData = new HashMap<>();

        // Common export fields
        exportData.put("id", archive.getId());
        exportData.put("title", archive.getTitle());
        exportData.put("description", archive.getDescription());
        exportData.put("standard", archive.getStandard());
        exportData.put("status", archive.getStatus());
        exportData.put("ownerId", archive.getOwnerId());
        exportData.put("createdAt", archive.getCreatedAt());
        exportData.put("updatedAt", archive.getUpdatedAt());

        // Standard-specific export
        exportStandard(archive, exportData);

        return exportData;
    }

    @Override
    public Map<String, Object> transformToStandard(Archive archive) {
        Map<String, Object> transformed = new HashMap<>();

        // Base transformation
        transformed.put("standardName", getStandardName());
        transformed.put("archiveId", archive.getId());
        transformed.put("title", archive.getTitle());

        // Standard-specific transformation
        addStandardSpecificFields(archive, transformed);

        return transformed;
    }

    /**
     * Perform standard-specific validation
     */
    protected abstract void validateStandard(Archive archive, ValidationResult result);

    /**
     * Add standard-specific fields to export
     */
    protected abstract void exportStandard(Archive archive, Map<String, Object> exportData);

    /**
     * Add standard-specific transformation fields
     */
    protected abstract void addStandardSpecificFields(Archive archive, Map<String, Object> transformed);
}
