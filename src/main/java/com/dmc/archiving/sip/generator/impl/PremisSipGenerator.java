package com.dmc.archiving.sip.generator.impl;

import com.dmc.archiving.sip.generator.AbstractSipGenerator;
import com.dmc.archiving.sip.generator.SipSnapshot;
import com.dmc.archiving.sip.input.FileMetadataInput;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PremisSipGenerator extends AbstractSipGenerator {

    public PremisSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "PREMIS";
    }

    @Override
    public Map<String, Object> buildPackage(SipSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "PREMIS");
        pkg.put("sipId", s.id());
        pkg.put("title", s.title());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        Map<String, Object> premisObject = new LinkedHashMap<>();
        premisObject.put("objectIdentifier", s.id().toString());
        premisObject.put("objectCategory", "representation");

        Map<String, Object> premisEvent = new LinkedHashMap<>();
        premisEvent.put("eventType", "creation");
        premisEvent.put("eventDateTime", s.createdAt());

        Map<String, Object> premisAgent = new LinkedHashMap<>();
        premisAgent.put("agentType", "software");

        if (s.hasRootElement()) {
            premisObject.put("entityName", s.entityName());
            premisObject.put("entityType", s.entityType());
            premisObject.put("title", s.elementTitle());

            if (s.hasFields()) {
                premisObject.put("significantProperties", s.fields());
            }

            premisAgent.put("agentIdentifier", s.elementCreatedBy());
        }

        pkg.put("premis:object", premisObject);
        pkg.put("premis:event", premisEvent);
        pkg.put("premis:agent", premisAgent);
        return pkg;
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("objectIdentifierType", "SHA-256");
        m.put("objectIdentifierValue", c.checksum() != null ? c.checksum() : c.id());
        m.put("objectCategory", "File");
        m.put("preservationLevelType", "full");
        m.put("preservationLevelValue", "full preservation");
        m.put("preservationLevelRole", "requirement");
        m.put("preservationLevelRationale", "Default preservation policy");
        m.put("preservationLevelDateAssigned", c.date());
        m.put("significantPropertiesType", "content");
        m.put("significantPropertiesValue", "All content preserved");
        m.put("originalName", c.name());
        return m;
    }
}
