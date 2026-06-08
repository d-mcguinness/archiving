package com.dmc.archiving.sip.generator.impl;

import com.dmc.archiving.sip.generator.AbstractSipGenerator;
import com.dmc.archiving.sip.generator.SipSnapshot;
import com.dmc.archiving.sip.input.FileMetadataInput;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class IsadgSipGenerator extends AbstractSipGenerator {

    public IsadgSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "ISADG";
    }

    @Override
    public Map<String, Object> buildPackage(SipSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "ISAD(G)");
        pkg.put("sipId", s.id());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        Map<String, Object> identityStatement = new LinkedHashMap<>();
        identityStatement.put("referenceCode", s.id().toString());
        identityStatement.put("title", s.title());
        identityStatement.put("date", s.createdAt());
        identityStatement.put("levelOfDescription", "collection");
        identityStatement.put("extentAndMedium", "electronic");

        Map<String, Object> contextArea = new LinkedHashMap<>();
        Map<String, Object> contentStructure = new LinkedHashMap<>();
        contentStructure.put("scopeAndContent", s.description());

        if (s.hasRootElement()) {
            identityStatement.put("entityName", s.entityName());
            identityStatement.put("entityType", s.entityType());

            contextArea.put("creatorName", s.elementCreatedBy());
            contextArea.put("elementIdentifier", s.elementIdentifier());

            if (s.hasFields()) {
                contentStructure.put("fields", s.fields());
            }
        }

        pkg.put("identityStatement", identityStatement);
        pkg.put("contextArea", contextArea);
        pkg.put("contentStructure", contentStructure);
        return pkg;
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("descriptionID", c.id());
        m.put("levelOfDescription", c.count() > 1 ? "File" : "Item");
        return m;
    }
}
