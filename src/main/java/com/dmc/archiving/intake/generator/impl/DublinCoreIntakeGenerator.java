package com.dmc.archiving.intake.generator.impl;

import com.dmc.archiving.intake.generator.AbstractIntakeGenerator;
import com.dmc.archiving.intake.generator.IntakeSnapshot;
import com.dmc.archiving.intake.input.FileMetadataInput;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DublinCoreIntakeGenerator extends AbstractIntakeGenerator {

    public DublinCoreIntakeGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "DUBLIN_CORE";
    }

    @Override
    public Map<String, Object> buildPackage(IntakeSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "DUBLIN_CORE");
        pkg.put("sipId", s.id());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        pkg.put("dc:title", s.title());
        pkg.put("dc:description", s.description());
        pkg.put("dc:date", s.createdAt());

        if (s.hasRootElement()) {
            pkg.put("dc:creator", s.elementCreatedBy());
            pkg.put("dc:type", s.entityType());
            pkg.put("dc:identifier", s.elementIdentifier());

            if (s.hasFields()) {
                pkg.put("dc:elements", s.fields());
            }
        }

        return pkg;
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("resourceIdentifier", c.id());
        m.put("resourceType", mapContentType(c.type()));
        return m;
    }
}
