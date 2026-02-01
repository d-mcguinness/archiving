package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for MODS (Metadata Object Description Schema)
 */
@Component
public class ModsStrategy extends AbstractArchiveStrategy {

    @Override
    public String getStandardName() {
        return "MODS";
    }

    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        // MODS is flexible but requires basic bibliographic info
        if (archive.getTitle() == null || archive.getTitle().isEmpty()) {
            result.addError("MODS requires titleInfo");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        // MODS structure for bibliographic metadata
        Map<String, Object> titleInfo = new HashMap<>();
        titleInfo.put("title", archive.getTitle());

        Map<String, Object> originInfo = new HashMap<>();
        originInfo.put("dateCreated", archive.getCreatedAt());
        originInfo.put("dateModified", archive.getUpdatedAt());

        Map<String, Object> physicalDescription = new HashMap<>();
        physicalDescription.put("form", "electronic");
        physicalDescription.put("internetMediaType", "application/json");

        Map<String, Object> abstractInfo = new HashMap<>();
        abstractInfo.put("content", archive.getDescription());

        Map<String, Object> identifier = new HashMap<>();
        identifier.put("type", "local");
        identifier.put("value", archive.getId());

        Map<String, Object> typeOfResource = new HashMap<>();
        typeOfResource.put("content", "mixed material");

        exportData.put("titleInfo", titleInfo);
        exportData.put("originInfo", originInfo);
        exportData.put("physicalDescription", physicalDescription);
        exportData.put("abstract", abstractInfo);
        exportData.put("identifier", identifier);
        exportData.put("typeOfResource", typeOfResource);
        exportData.put("standardVersion", "MODS 3.7");
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("bibliographicMetadata", true);
        transformed.put("modsVersion", "3.7");
    }

    @Override
    public Archive importArchive(Map<String, Object> data) {
        Archive archive = new Archive();
        if (data.containsKey("titleInfo")) {
            Map<?, ?> titleInfo = (Map<?, ?>) data.get("titleInfo");
            if (titleInfo.containsKey("title")) {
                archive.setTitle(titleInfo.get("title").toString());
            }
        }
        if (data.containsKey("abstract")) {
            Map<?, ?> abstractInfo = (Map<?, ?>) data.get("abstract");
            if (abstractInfo.containsKey("content")) {
                archive.setDescription(abstractInfo.get("content").toString());
            }
        }
        return archive;
    }

    @Override
    public Map<String, String> getMetadataRequirements() {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("titleInfo", "Title of the resource");
        requirements.put("name", "Name of entity associated with resource");
        requirements.put("typeOfResource", "General type (text, image, etc.)");
        requirements.put("genre", "Specific kind of resource");
        requirements.put("originInfo", "Publication/creation information");
        requirements.put("language", "Language of content");
        requirements.put("physicalDescription", "Physical characteristics");
        requirements.put("abstract", "Summary of content");
        requirements.put("subject", "Subject terms");
        requirements.put("identifier", "Unique identifier");
        requirements.put("location", "Location information");
        requirements.put("accessCondition", "Access restrictions");
        return requirements;
    }
}
