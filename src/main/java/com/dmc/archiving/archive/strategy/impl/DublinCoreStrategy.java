package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for Dublin Core - Simple metadata standard (ISO 15836)
 */
@Component
public class DublinCoreStrategy extends AbstractArchiveStrategy {

    @Override
    public String getStandardName() {
        return "DUBLIN_CORE";
    }

    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        // Dublin Core has minimal requirements - very flexible
        if (archive.getTitle() == null || archive.getTitle().isEmpty()) {
            result.addError("Dublin Core requires a title (dc:title)");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        exportData.put("standardReference", "Dublin Core Metadata Element Set, Version 1.1");
        exportData.put("standardName", "Dublin Core");
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("metadataSchema", "Dublin Core");
        transformed.put("dcFormat", "qualified");
    }

    @Override
    public Archive importArchive(Map<String, Object> data) {
        Archive archive = new Archive();
        if (data.containsKey("dublinCore")) {
            Map<?, ?> dc = (Map<?, ?>) data.get("dublinCore");
            if (dc.containsKey("dc:title")) {
                archive.setTitle(dc.get("dc:title").toString());
            }
            if (dc.containsKey("dc:description")) {
                archive.setDescription(dc.get("dc:description").toString());
            }
        }
        return archive;
    }

    @Override
    public Map<String, String> getMetadataRequirements() {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("dc:title", "Title of the resource");
        requirements.put("dc:creator", "Entity responsible for making the resource");
        requirements.put("dc:subject", "Topic of the resource");
        requirements.put("dc:description", "Description of the resource");
        requirements.put("dc:date", "Date associated with the resource");
        requirements.put("dc:type", "Nature or genre of the resource");
        requirements.put("dc:format", "File format or physical medium");
        requirements.put("dc:identifier", "Unambiguous reference to the resource");
        requirements.put("dc:language", "Language of the resource");
        requirements.put("dc:rights", "Rights held in and over the resource");
        return requirements;
    }
}
