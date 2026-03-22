package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EarkStrategy extends AbstractArchiveStrategy {

    @Override
    public String getStandardName() {
        return "EARK";
    }

    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        if (archive.getTitle() == null || archive.getTitle().isEmpty()) {
            result.addError("E-ARK requires a title");
        }
        if (archive.getDescription() == null || archive.getDescription().isEmpty()) {
            result.addWarning("E-ARK recommends including a description");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        exportData.put("standardVersion", "E-ARK CSIP 2.1.0");
        exportData.put("mets", Map.of(
            "packageID", archive.getId(),
            "title", archive.getTitle(),
            "description", archive.getDescription() != null ? archive.getDescription() : "",
            "contentInformationType", "MIXED",
            "profile", "https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml",
            "oaisPackageType", "AIP",
            "metsProfile", "https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml"
        ));
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("packageType", "E-ARK CSIP");
        transformed.put("contentInformationType", "MIXED");
        transformed.put("profile", "https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml");
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
        Map<String, String> requirements = new HashMap<>();
        requirements.put("packageID", "Unique package identifier");
        requirements.put("title", "Title of the information package");
        requirements.put("contentInformationType", "Type of content information (MIXED, OTHER, etc.)");
        requirements.put("profile", "E-ARK CSIP profile reference");
        requirements.put("oaisPackageType", "OAIS package type (SIP, AIP, DIP)");
        requirements.put("metsProfile", "METS profile reference URL");
        return requirements;
    }
}
