package com.dmc.archiving.intake.generator.impl;

import com.dmc.archiving.intake.generator.AbstractIntakeGenerator;
import com.dmc.archiving.intake.generator.IntakeSnapshot;
import com.dmc.archiving.intake.input.FileMetadataInput;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EadIntakeGenerator extends AbstractIntakeGenerator {

    public EadIntakeGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "EAD";
    }

    @Override
    public Map<String, Object> buildPackage(IntakeSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "EAD");
        pkg.put("sipId", s.id());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        Map<String, Object> eadHeader = new LinkedHashMap<>();
        eadHeader.put("eadid", s.id().toString());
        eadHeader.put("titleproper", s.title());

        Map<String, Object> archdesc = new LinkedHashMap<>();
        archdesc.put("level", "collection");

        Map<String, Object> did = new LinkedHashMap<>();
        did.put("unittitle", s.title());
        did.put("unitdate", s.createdAt());
        did.put("abstract", s.description());

        if (s.hasRootElement()) {
            did.put("unitid", s.elementIdentifier());
            did.put("origination", s.elementCreatedBy());
            archdesc.put("entityName", s.entityName());
            archdesc.put("entityType", s.entityType());

            if (s.hasFields()) {
                did.put("fields", s.fields());
            }
        }

        pkg.put("ead:header", eadHeader);
        pkg.put("ead:archdesc", archdesc);
        pkg.put("ead:did", did);
        return pkg;
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("eadID", c.id());
        m.put("audience", "external");
        m.put("relatedEncoding", "Dublin Core");
        m.put("lang", "eng");
        m.put("script", "Latn");
        m.put("base", "");
        return m;
    }
}
