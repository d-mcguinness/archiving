package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Default strategy for standards without specific implementation
 * Used for: Dublin Core, METS, EAD, BagIt, ISAD(G), MODS
 */
@Component
public class DefaultArchiveStrategy extends AbstractArchiveStrategy {

    private String standardName = "DEFAULT";

    public DefaultArchiveStrategy() {
    }

    public DefaultArchiveStrategy(String standardName) {
        this.standardName = standardName;
    }

    @Override
    public String getStandardName() {
        return standardName;
    }

    public void setStandardName(String standardName) {
        this.standardName = standardName;
    }

    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        // Basic validation only
        if (archive.getStandard() == null) {
            result.addWarning("Archive standard not specified");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        // Generic export
        exportData.put("standardName", standardName);
        exportData.put("content", archive.getContent());
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("standard", standardName);
        transformed.put("genericFormat", true);
    }

    @Override
    public Archive importArchive(Map<String, Object> data) {
        Archive archive = new Archive();
        // Generic import
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
        Map<String, String> requirements = new HashMap<>();
        requirements.put("title", "Title of the archive");
        requirements.put("description", "Description of the archive");
        requirements.put("creator", "Creator of the archive");
        requirements.put("date", "Creation date");
        requirements.put("identifier", "Unique identifier");
        return requirements;
    }
}
