package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for NOARK5 (Norwegian archival standard)
 */
@Component
public class Noark5Strategy extends AbstractArchiveStrategy {

    @Override
    public String getStandardName() {
        return "NOARK5";
    }

    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        // NOARK5-specific validation
        // Check for required NOARK5 metadata
        if (archive.getDescription() == null || archive.getDescription().isEmpty()) {
            result.addWarning("NOARK5 recommends including a description");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        exportData.put("standardReference", "NOARK5 v5.0");
        exportData.put("standardName", "Norsk Arkivstandard 5");
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("arkivType", "Arkiv");
        transformed.put("dokumentmedium", "Elektronisk arkiv");
    }

    @Override
    public Archive importArchive(Map<String, Object> data) {
        // NOARK5-specific import logic
        Archive archive = new Archive();
        // Implementation would parse NOARK5 format
        return archive;
    }

    @Override
    public Map<String, String> getMetadataRequirements() {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("systemID", "Unique system identifier");
        requirements.put("tittel", "Title of the archive");
        requirements.put("beskrivelse", "Description");
        requirements.put("dokumentmedium", "Document medium (electronic/physical)");
        requirements.put("opprettetDato", "Creation date");
        requirements.put("opprettetAv", "Created by");
        return requirements;
    }
}
