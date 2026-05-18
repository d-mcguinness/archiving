package com.dmc.archiving.sip.generator.impl;

import com.dmc.archiving.sip.generator.AbstractSipGenerator;
import com.dmc.archiving.sip.generator.SipSnapshot;
import com.dmc.archiving.sip.input.FileMetadataInput;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BagitSipGenerator extends AbstractSipGenerator {

    public BagitSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "BAGIT";
    }

    @Override
    public Map<String, Object> buildPackage(SipSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "BAGIT");
        pkg.put("sipId", s.id());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        Map<String, Object> bagitInfo = new LinkedHashMap<>();
        bagitInfo.put("BagIt-Version", "1.0");
        bagitInfo.put("Tag-File-Character-Encoding", "UTF-8");
        bagitInfo.put("Source-Organization", s.title());
        bagitInfo.put("Bagging-Date", s.createdAt());
        bagitInfo.put("Bag-Count", "1 of 1");

        Map<String, Object> manifest = new LinkedHashMap<>();
        Map<String, Object> tagManifest = new LinkedHashMap<>();
        Map<String, Object> dataReferences = new LinkedHashMap<>();

        if (s.hasRootElement()) {
            dataReferences.put("entityName", s.entityName());
            dataReferences.put("entityType", s.entityType());
            dataReferences.put("title", s.elementTitle());
            dataReferences.put("identifier", s.elementIdentifier());

            if (s.hasFields()) {
                dataReferences.put("fields", s.fields());
            }
        }

        pkg.put("bagit-info", bagitInfo);
        pkg.put("manifest", manifest);
        pkg.put("tag-manifest", tagManifest);
        pkg.put("data", dataReferences);
        return pkg;
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        String bagName = c.name().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        long size = c.fileSizeBytes() != null ? c.fileSizeBytes() : 0L;
        Map<String, String> m = new LinkedHashMap<>();
        m.put("bagName", bagName);
        m.put("payloadOxum", size + "." + c.count());
        m.put("bagSize", humanReadableSize(size));
        m.put("isComplete", "true");
        m.put("isValid", "true");
        return m;
    }
}
