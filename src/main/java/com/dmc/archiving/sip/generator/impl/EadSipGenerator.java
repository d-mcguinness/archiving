package com.dmc.archiving.sip.generator.impl;

import com.dmc.archiving.sip.generator.AbstractSipGenerator;
import com.dmc.archiving.sip.generator.SipSnapshot;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EadSipGenerator extends AbstractSipGenerator {

    public EadSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "EAD";
    }

    @Override
    public Map<String, Object> buildPackage(SipSnapshot s) {
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
}
