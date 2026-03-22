package com.dmc.archiving.aip.generator.impl;

import com.dmc.archiving.aip.generator.AbstractAipGenerator;
import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultAipGenerator extends AbstractAipGenerator {

    public DefaultAipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "DEFAULT";
    }

    @Override
    public Map<String, Object> buildPackage(Aip aip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", aip.getStandard() != null ? aip.getStandard().name() : "UNKNOWN");
        pkg.put("aipId", aip.getId());
        pkg.put("title", aip.getTitle());
        pkg.put("description", aip.getDescription());
        pkg.put("status", aip.getStatus().name());
        pkg.put("createdAt", aip.getCreatedAtString());

        Element root = aip.getRootElement();
        if (root != null) {
            Map<String, Object> rootElement = new LinkedHashMap<>();
            rootElement.put("elementIdentifier", root.getElementIdentifier());
            rootElement.put("entityName", root.getEntityName());
            rootElement.put("entityType", root.getEntityType());
            rootElement.put("title", root.getTitle());
            rootElement.put("description", root.getDescription());

            List<Field> fields = root.getFields();
            if (fields != null && !fields.isEmpty()) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                for (Field field : fields) {
                    fieldMap.put(field.getName(), field.getValue());
                }
                rootElement.put("fields", fieldMap);
            }

            pkg.put("rootElement", rootElement);
        }

        return pkg;
    }
}
