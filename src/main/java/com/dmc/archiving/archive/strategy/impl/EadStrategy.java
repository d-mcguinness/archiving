package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for EAD (Encoded Archival Description)
 */
@Component
public class EadStrategy extends AbstractArchiveStrategy {

    @Override
    public String getStandardName() {
        return "EAD";
    }

    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        // EAD requires finding aid structure
        if (archive.getTitle() == null || archive.getTitle().isEmpty()) {
            result.addError("EAD requires unittitle");
        }
        if (archive.getDescription() == null || archive.getDescription().isEmpty()) {
            result.addWarning("EAD recommends scope and content note");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        exportData.put("standardReference", "EAD3 1.1.1");
        exportData.put("standardName", "Encoded Archival Description");
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("findingAid", true);
        transformed.put("level", "collection");
    }

    @Override
    public Archive importArchive(Map<String, Object> data) {
        Archive archive = new Archive();
        if (data.containsKey("archdesc")) {
            Map<?, ?> archdesc = (Map<?, ?>) data.get("archdesc");
            if (archdesc.containsKey("unittitle")) {
                archive.setTitle(archdesc.get("unittitle").toString());
            }
            if (archdesc.containsKey("scopecontent")) {
                archive.setDescription(archdesc.get("scopecontent").toString());
            }
        }
        return archive;
    }

    @Override
    public Map<String, String> getMetadataRequirements() {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("eadid", "Unique identifier for the finding aid");
        requirements.put("unittitle", "Title of the archival unit");
        requirements.put("unitdate", "Date(s) of the materials");
        requirements.put("physdesc", "Physical description");
        requirements.put("origination", "Creator(s) of the materials");
        requirements.put("scopecontent", "Scope and content note");
        requirements.put("arrangement", "Organization of materials");
        requirements.put("accessrestrict", "Access restrictions");
        requirements.put("userestrict", "Use restrictions");
        return requirements;
    }
}
