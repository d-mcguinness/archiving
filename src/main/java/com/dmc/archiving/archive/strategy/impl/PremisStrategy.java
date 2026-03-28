package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for PREMIS (Preservation Metadata)
 */
@Component
public class PremisStrategy extends AbstractArchiveStrategy {

    @Override
    public String getStandardName() {
        return "PREMIS";
    }

    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        // PREMIS focuses on preservation metadata for digital objects
        if (archive.getId() == null) {
            result.addError("PREMIS requires object identifier");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        exportData.put("standardReference", "PREMIS 3.0");
        exportData.put("standardName", "Preservation Metadata: Implementation Strategies");
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("objectCategory", "representation");
        transformed.put("preservationLevel", "full");
        transformed.put("objectIdentifierType", "UUID");
    }

    @Override
    public Archive importArchive(Map<String, Object> data) {
        Archive archive = new Archive();
        return archive;
    }

    @Override
    public Map<String, String> getMetadataRequirements() {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("objectIdentifier", "Unique identifier for the digital object");
        requirements.put("objectCategory", "Type of object (file, bitstream, representation)");
        requirements.put("preservationLevel", "Level of preservation");
        requirements.put("significantProperties", "Properties to preserve");
        requirements.put("originalName", "Original filename");
        requirements.put("size", "Size in bytes");
        requirements.put("format", "File format");
        return requirements;
    }
}
