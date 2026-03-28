package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for METS (Metadata Encoding and Transmission Standard)
 */
@Component
public class MetsStrategy extends AbstractArchiveStrategy {

    @Override
    public String getStandardName() {
        return "METS";
    }

    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        // METS requires structured metadata sections
        if (archive.getTitle() == null || archive.getTitle().isEmpty()) {
            result.addError("METS requires descriptive metadata (dmdSec)");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        exportData.put("standardReference", "METS 1.12");
        exportData.put("standardName", "Metadata Encoding and Transmission Standard");
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("structuralMetadata", true);
        transformed.put("packageFormat", "METS");
    }

    @Override
    public Archive importArchive(Map<String, Object> data) {
        Archive archive = new Archive();
        if (data.containsKey("dmdSec")) {
            Map<?, ?> dmd = (Map<?, ?>) data.get("dmdSec");
            if (dmd.containsKey("title")) {
                archive.setTitle(dmd.get("title").toString());
            }
        }
        return archive;
    }

    @Override
    public Map<String, String> getMetadataRequirements() {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("metsHdr", "METS header with creation/modification dates");
        requirements.put("dmdSec", "Descriptive metadata section");
        requirements.put("amdSec", "Administrative metadata section");
        requirements.put("fileSec", "File section listing all files");
        requirements.put("structMap", "Structural map showing organization");
        requirements.put("behaviorSec", "Behaviors associated with content (optional)");
        return requirements;
    }
}
