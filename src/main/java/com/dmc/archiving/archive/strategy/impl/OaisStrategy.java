package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for OAIS (Open Archival Information System) - ISO 14721
 */
@Component
public class OaisStrategy extends AbstractArchiveStrategy {

    @Override
    public String getStandardName() {
        return "OAIS";
    }

    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        // OAIS-specific validation
        // OAIS requires specific information packages
        if (archive.getContent() == null || archive.getContent().isEmpty()) {
            result.addError("OAIS requires content information");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        // OAIS-specific export format (SIP/AIP/DIP structure)
        Map<String, Object> informationPackage = new HashMap<>();
        informationPackage.put("packageType", "AIP"); // Archival Information Package
        informationPackage.put("contentInformation", archive.getContent());
        informationPackage.put("preservationDescriptionInformation", Map.of(
            "reference", Map.of("identifier", archive.getId()),
            "context", Map.of("title", archive.getTitle()),
            "provenance", Map.of("createdAt", archive.getCreatedAt())
        ));

        exportData.put("informationPackage", informationPackage);
        exportData.put("standardVersion", "ISO 14721:2012");
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("packageType", "AIP");
        transformed.put("contentDataObject", archive.getContent());
        transformed.put("preservationLevel", "full");
    }

    @Override
    public Archive importArchive(Map<String, Object> data) {
        // OAIS-specific import logic
        Archive archive = new Archive();
        // Implementation would parse OAIS information package
        return archive;
    }

    @Override
    public Map<String, String> getMetadataRequirements() {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("identifier", "Unique identifier for the AIP");
        requirements.put("title", "Title of the information package");
        requirements.put("contentInformation", "The actual content being preserved");
        requirements.put("preservationDescriptionInformation", "PDI metadata");
        requirements.put("packagingInformation", "How the package is structured");
        requirements.put("descriptiveInformation", "Descriptive metadata");
        return requirements;
    }
}
