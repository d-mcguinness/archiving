package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for ISAD(G) - General International Standard Archival Description
 */
@Component
public class IsadgStrategy extends AbstractArchiveStrategy {

    @Override
    public String getStandardName() {
        return "ISADG";
    }

    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        // ISAD(G) has 6 mandatory elements
        if (archive.getId() == null) {
            result.addError("ISAD(G) requires reference code");
        }
        if (archive.getTitle() == null || archive.getTitle().isEmpty()) {
            result.addError("ISAD(G) requires title");
        }
        if (archive.getOwnerId() == null) {
            result.addError("ISAD(G) requires creator information");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        // ISAD(G) 26 elements organized in 7 areas

        // Identity Statement Area
        Map<String, Object> identityArea = new HashMap<>();
        identityArea.put("referenceCode", archive.getId());
        identityArea.put("title", archive.getTitle());
        identityArea.put("date", archive.getCreatedAt());
        identityArea.put("levelOfDescription", "Fonds");
        identityArea.put("extentAndMedium", "Electronic records");

        // Context Area
        Map<String, Object> contextArea = new HashMap<>();
        contextArea.put("nameOfCreator", archive.getOwnerId());
        contextArea.put("administrativeHistory", archive.getDescription());

        // Content and Structure Area
        Map<String, Object> contentArea = new HashMap<>();
        contentArea.put("scopeAndContent", archive.getDescription());

        // Conditions of Access and Use Area
        Map<String, Object> accessArea = new HashMap<>();
        accessArea.put("conditionsGoverningAccess", "Restricted");

        exportData.put("identityStatementArea", identityArea);
        exportData.put("contextArea", contextArea);
        exportData.put("contentAndStructureArea", contentArea);
        exportData.put("conditionsOfAccessAndUseArea", accessArea);
        exportData.put("standardVersion", "ISAD(G) 2nd Edition");
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("levelOfDescription", "Fonds");
        transformed.put("descriptiveStandard", "ISAD(G)");
    }

    @Override
    public Archive importArchive(Map<String, Object> data) {
        Archive archive = new Archive();
        if (data.containsKey("identityStatementArea")) {
            Map<?, ?> identity = (Map<?, ?>) data.get("identityStatementArea");
            if (identity.containsKey("title")) {
                archive.setTitle(identity.get("title").toString());
            }
        }
        return archive;
    }

    @Override
    public Map<String, String> getMetadataRequirements() {
        Map<String, String> requirements = new HashMap<>();
        // Identity Statement Area
        requirements.put("3.1.1 Reference code(s)", "Unique identifier (MANDATORY)");
        requirements.put("3.1.2 Title", "Formal title (MANDATORY)");
        requirements.put("3.1.3 Date(s)", "Date of creation (MANDATORY)");
        requirements.put("3.1.4 Level of description", "Level in hierarchy (MANDATORY)");
        requirements.put("3.1.5 Extent and medium", "Physical characteristics (MANDATORY)");
        // Context Area
        requirements.put("3.2.1 Name of creator(s)", "Creator entity (MANDATORY)");
        requirements.put("3.2.2 Administrative history", "History of creator");
        requirements.put("3.2.3 Archival history", "Custodial history");
        requirements.put("3.2.4 Immediate source", "Source of acquisition");
        return requirements;
    }
}
