package com.dmc.archiving.sip.generator.impl;

import com.dmc.archiving.sip.generator.AbstractSipGenerator;
import com.dmc.archiving.sip.generator.SipSnapshot;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EarkSipGenerator extends AbstractSipGenerator {

    public EarkSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "EARK";
    }

    @Override
    public Map<String, Object> buildPackage(SipSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "EARK");
        pkg.put("standardVersion", "E-ARK CSIP 2.1.0");
        pkg.put("sipId", s.id());
        pkg.put("title", s.title());
        pkg.put("description", s.description());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        Map<String, Object> metsHeader = new LinkedHashMap<>();
        metsHeader.put("packageID", "SIP-" + s.id());
        metsHeader.put("contentInformationType", "MIXED");
        metsHeader.put("profile", "https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml");
        metsHeader.put("oaisPackageType", "SIP");

        Map<String, Object> structMap = new LinkedHashMap<>();
        structMap.put("type", "physical");
        structMap.put("label", "CSIP");

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

            structMap.put("rootElement", rootElement);
        }

        pkg.put("metsHeader", metsHeader);
        pkg.put("structMap", structMap);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("descriptive", Map.of("type", "EAD", "status", "current"));
        metadata.put("administrative", Map.of("type", "PREMIS", "status", "current"));
        pkg.put("metadata", metadata);

        pkg.put("representations", List.of(
            Map.of("id", "rep-001", "type", "original", "label", "Original representation")
        ));

        return pkg;
    }
}
