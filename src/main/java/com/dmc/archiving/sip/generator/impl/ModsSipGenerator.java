package com.dmc.archiving.sip.generator.impl;

import com.dmc.archiving.sip.generator.AbstractSipGenerator;
import com.dmc.archiving.sip.generator.SipSnapshot;
import com.dmc.archiving.sip.input.FileMetadataInput;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ModsSipGenerator extends AbstractSipGenerator {

    public ModsSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "MODS";
    }

    @Override
    public Map<String, Object> buildPackage(SipSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "MODS");
        pkg.put("sipId", s.id());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        Map<String, Object> titleInfo = new LinkedHashMap<>();
        titleInfo.put("title", s.title());
        titleInfo.put("subTitle", s.description());

        Map<String, Object> name = new LinkedHashMap<>();
        Map<String, Object> originInfo = new LinkedHashMap<>();
        originInfo.put("dateCreated", s.createdAt());

        if (s.hasRootElement()) {
            name.put("namePart", s.elementCreatedBy());
            name.put("role", "creator");

            originInfo.put("publisher", s.entityName());

            Map<String, Object> subject = new LinkedHashMap<>();
            subject.put("entityType", s.entityType());
            subject.put("identifier", s.elementIdentifier());
            pkg.put("mods:subject", subject);

            if (s.hasFields()) {
                pkg.put("mods:extension", s.fields());
            }
        }

        pkg.put("mods:titleInfo", titleInfo);
        pkg.put("mods:name", name);
        pkg.put("mods:originInfo", originInfo);
        return pkg;
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("modsID", c.id());
        m.put("version", "3.8");
        return m;
    }
}
