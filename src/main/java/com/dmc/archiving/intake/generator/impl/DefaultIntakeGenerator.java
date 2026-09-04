package com.dmc.archiving.intake.generator.impl;

import com.dmc.archiving.intake.generator.AbstractIntakeGenerator;
import com.dmc.archiving.intake.generator.IntakeSnapshot;
import com.dmc.archiving.intake.input.FileMetadataInput;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DefaultIntakeGenerator extends AbstractIntakeGenerator {

    public DefaultIntakeGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "DEFAULT";
    }

    @Override
    public Map<String, Object> buildPackage(IntakeSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", s.standard());
        pkg.put("sipId", s.id());
        pkg.put("title", s.title());
        pkg.put("description", s.description());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        if (s.hasRootElement()) {
            Map<String, Object> rootElement = new LinkedHashMap<>();
            rootElement.put("elementIdentifier", s.elementIdentifier());
            rootElement.put("entityName", s.entityName());
            rootElement.put("entityType", s.entityType());
            rootElement.put("title", s.elementTitle());
            rootElement.put("description", s.elementDescription());

            if (s.hasFields()) {
                rootElement.put("fields", s.fields());
            }

            pkg.put("rootElement", rootElement);
        }

        return pkg;
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", c.id());
        m.put("title", c.name());
        m.put("createdDate", c.date());
        m.put("createdBy", c.user());
        return m;
    }
}
