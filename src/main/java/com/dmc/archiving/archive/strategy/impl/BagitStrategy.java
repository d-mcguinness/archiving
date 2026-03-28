package com.dmc.archiving.archive.strategy.impl;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.AbstractArchiveStrategy;
import com.dmc.archiving.archive.strategy.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for BagIt - File packaging format (RFC 8493)
 */
@Component
public class BagitStrategy extends AbstractArchiveStrategy {

    @Override
    public String getStandardName() {
        return "BAGIT";
    }

    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        // BagIt requires payload and manifest
        if (archive.getContent() == null || archive.getContent().isEmpty()) {
            result.addError("BagIt requires payload content");
        }
    }

    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        exportData.put("standardReference", "BagIt 1.0 (RFC 8493)");
        exportData.put("standardName", "BagIt File Packaging Format");
    }

    @Override
    protected void addStandardSpecificFields(Archive archive, Map<String, Object> transformed) {
        transformed.put("packageType", "BagIt");
        transformed.put("checksumAlgorithm", "sha256");
    }

    @Override
    public Archive importArchive(Map<String, Object> data) {
        Archive archive = new Archive();
        if (data.containsKey("bag-info.txt")) {
            Map<?, ?> bagInfo = (Map<?, ?>) data.get("bag-info.txt");
            if (bagInfo.containsKey("External-Description")) {
                archive.setDescription(bagInfo.get("External-Description").toString());
            }
        }
        if (data.containsKey("payload")) {
            archive.setContent(data.get("payload").toString());
        }
        return archive;
    }

    @Override
    public Map<String, String> getMetadataRequirements() {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("bagit.txt", "BagIt version and encoding");
        requirements.put("bag-info.txt", "Metadata about the bag");
        requirements.put("manifest-<algorithm>.txt", "Checksums for payload files");
        requirements.put("data/", "Directory containing payload files");
        requirements.put("Source-Organization", "Organization creating the bag");
        requirements.put("Bagging-Date", "Date the bag was created");
        requirements.put("External-Description", "Brief description");
        requirements.put("Payload-Oxum", "Octetstream sum (bytes.files)");
        return requirements;
    }
}
