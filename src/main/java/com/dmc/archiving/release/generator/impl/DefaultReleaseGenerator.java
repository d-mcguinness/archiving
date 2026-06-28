package com.dmc.archiving.release.generator.impl;

import com.dmc.archiving.release.generator.AbstractReleaseGenerator;
import com.dmc.archiving.release.model.Release;
import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultReleaseGenerator extends AbstractReleaseGenerator {

    public DefaultReleaseGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "DEFAULT";
    }

    @Override
    public Map<String, Object> buildPackage(Release dip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", dip.getStandard() != null ? dip.getStandard().name() : "UNKNOWN");
        pkg.put("dipId", dip.getId());
        pkg.put("title", dip.getTitle());
        pkg.put("description", dip.getDescription());
        pkg.put("status", dip.getStatus().name());
        pkg.put("createdAt", dip.getCreatedAtString());

        Element root = dip.getRootElement();
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
