package com.dmc.archiving.sip.generator.impl;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.sip.generator.AbstractSipGenerator;
import com.dmc.archiving.sip.model.Sip;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
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
    public Map<String, Object> buildPackage(Sip sip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "PREMIS");
        pkg.put("sipId", sip.getId());
        pkg.put("title", sip.getTitle());
        pkg.put("status", sip.getStatus().name());
        pkg.put("createdAt", sip.getCreatedAtString());

        Map<String, Object> premisObject = new LinkedHashMap<>();
        premisObject.put("objectIdentifier", sip.getId().toString());
        premisObject.put("objectCategory", "representation");

        Map<String, Object> premisEvent = new LinkedHashMap<>();
        premisEvent.put("eventType", "creation");
        premisEvent.put("eventDateTime", sip.getCreatedAtString());

        Map<String, Object> premisAgent = new LinkedHashMap<>();
        premisAgent.put("agentType", "software");

        Element root = sip.getRootElement();
        if (root != null) {
            premisObject.put("entityName", root.getEntityName());
            premisObject.put("entityType", root.getEntityType());
            premisObject.put("title", root.getTitle());

            List<Field> fields = root.getFields();
            if (fields != null && !fields.isEmpty()) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                for (Field field : fields) {
                    fieldMap.put(field.getName(), field.getValue());
                }
                premisObject.put("significantProperties", fieldMap);
            }

            premisAgent.put("agentIdentifier", root.getCreatedBy());
        }

        pkg.put("premis:object", premisObject);
        pkg.put("premis:event", premisEvent);
        pkg.put("premis:agent", premisAgent);
        return pkg;
    }
}
