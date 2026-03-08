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
public class OaisSipGenerator extends AbstractSipGenerator {

    public OaisSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "OAIS";
    }

    @Override
    public Map<String, Object> buildPackage(Sip sip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "OAIS");
        pkg.put("sipId", sip.getId());
        pkg.put("title", sip.getTitle());
        pkg.put("status", sip.getStatus().name());
        pkg.put("createdAt", sip.getCreatedAtString());

        Map<String, Object> informationPackage = new LinkedHashMap<>();
        informationPackage.put("type", "SIP");
        informationPackage.put("title", sip.getTitle());
        informationPackage.put("description", sip.getDescription());

        Map<String, Object> contentInformation = new LinkedHashMap<>();
        Map<String, Object> preservationDescription = new LinkedHashMap<>();

        Element root = sip.getRootElement();
        if (root != null) {
            contentInformation.put("entityName", root.getEntityName());
            contentInformation.put("entityType", root.getEntityType());
            contentInformation.put("title", root.getTitle());

            List<Field> fields = root.getFields();
            if (fields != null && !fields.isEmpty()) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                for (Field field : fields) {
                    fieldMap.put(field.getName(), field.getValue());
                }
                contentInformation.put("dataObjects", fieldMap);
            }

            preservationDescription.put("createdAt", root.getCreatedAt() != null ? root.getCreatedAt().toString() : null);
            preservationDescription.put("createdBy", root.getCreatedBy());
            preservationDescription.put("status", root.getStatus());
        }

        informationPackage.put("contentInformation", contentInformation);
        informationPackage.put("preservationDescription", preservationDescription);
        pkg.put("informationPackage", informationPackage);
        return pkg;
    }
}
