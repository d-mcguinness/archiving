package com.dmc.archiving.archive.strategy;

import com.dmc.archiving.archive.model.Archive;

import java.util.Map;

/**
 * Strategy interface for archive operations based on archiving standards.
 * Each standard (NOARK5, OAIS, PREMIS, etc.) can have its own implementation.
 */
public interface ArchiveStrategy {

    /**
     * Validate archive data according to the standard's requirements
     * @param archive The archive to validate
     * @return Validation result with any errors
     */
    ValidationResult validate(Archive archive);

    /**
     * Export archive in the standard's format
     * @param archive The archive to export
     * @return Exported data as a Map
     */
    Map<String, Object> export(Archive archive);

    /**
     * Import archive from the standard's format
     * @param data The data to import
     * @return The imported archive
     */
    Archive importArchive(Map<String, Object> data);

    /**
     * Get metadata requirements for this standard
     * @return Map of required metadata fields and their descriptions
     */
    Map<String, String> getMetadataRequirements();

    /**
     * Transform archive data to standard-specific format
     * @param archive The archive to transform
     * @return Transformed data
     */
    Map<String, Object> transformToStandard(Archive archive);

    /**
     * Get the standard name this strategy handles
     * @return Standard name (e.g., "NOARK5", "OAIS")
     */
    String getStandardName();
}
