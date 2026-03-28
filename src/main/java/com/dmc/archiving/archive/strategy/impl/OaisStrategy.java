package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
        // OAIS requires either element tree or content
        if (archive.getRootElement() == null &&
            (archive.getContent() == null || archive.getContent().isEmpty())) {
            result.addWarning("OAIS recommends defining an information package structure");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        // Add OAIS-specific metadata
        exportData.put("standardReference", "ISO 14721:2012");
        exportData.put("standardName", "Open Archival Information System");

        // Determine package type from root element if available
        String packageType = "AIP";
        if (archive.getRootElement() != null) {
            String entityName = archive.getRootElement().getEntityName();
            if (entityName != null) {
                if (entityName.contains("Submission")) {
                    packageType = "SIP";
                } else if (entityName.contains("Dissemination")) {
                    packageType = "DIP";
                }
            }
        }
        exportData.put("packageType", packageType);

        // The element tree is already exported by AbstractArchiveStrategy.export()
        // under "rootElement" — no need to duplicate it here
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("packageType", "AIP");
        transformed.put("preservationLevel", "full");
    }

    @Override
    public Archive importArchive(Map<String, Object> data) {
        Archive archive = new Archive();
        if (data.containsKey("title")) {
            archive.setTitle(data.get("title").toString());
        }
        if (data.containsKey("description")) {
            archive.setDescription(data.get("description").toString());
        }
        return archive;
    }

    @Override
    public Map<String, String> getMetadataRequirements() {
        Map<String, String> requirements = new LinkedHashMap<>();
        requirements.put("identifier", "Unique identifier for the information package");
        requirements.put("title", "Title of the information package");
        requirements.put("contentInformation", "The actual content being preserved");
        requirements.put("preservationDescriptionInformation", "PDI metadata (provenance, context, reference, fixity, access rights)");
        requirements.put("packagingInformation", "How the package is structured");
        requirements.put("descriptiveInformation", "Descriptive metadata for discovery");
        return requirements;
    }
}
