package com.dmc.archiving.sip.generator.impl;

import com.dmc.archiving.sip.generator.AbstractSipGenerator;
import com.dmc.archiving.sip.generator.SipSnapshot;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DublinCoreSipGenerator extends AbstractSipGenerator {

    public DublinCoreSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "DUBLIN_CORE";
    }

    @Override
    public Map<String, Object> buildPackage(SipSnapshot s) {
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
}
