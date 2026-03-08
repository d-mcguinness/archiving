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
public class IsadgSipGenerator extends AbstractSipGenerator {

    public IsadgSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "ISADG";
    }

    @Override
    public Map<String, Object> buildPackage(Sip sip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "ISAD(G)");
        pkg.put("sipId", sip.getId());
        pkg.put("status", sip.getStatus().name());
        pkg.put("createdAt", sip.getCreatedAtString());

        Map<String, Object> identityStatement = new LinkedHashMap<>();
        identityStatement.put("referenceCode", sip.getId().toString());
        identityStatement.put("title", sip.getTitle());
        identityStatement.put("date", sip.getCreatedAtString());
        identityStatement.put("levelOfDescription", "collection");
        identityStatement.put("extentAndMedium", "electronic");

        Map<String, Object> contextArea = new LinkedHashMap<>();
        Map<String, Object> contentStructure = new LinkedHashMap<>();
        contentStructure.put("scopeAndContent", sip.getDescription());

        Element root = sip.getRootElement();
        if (root != null) {
            identityStatement.put("entityName", root.getEntityName());
            identityStatement.put("entityType", root.getEntityType());

            contextArea.put("creatorName", root.getCreatedBy());
            contextArea.put("elementIdentifier", root.getElementIdentifier());

            List<Field> fields = root.getFields();
            if (fields != null && !fields.isEmpty()) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                for (Field field : fields) {
                    fieldMap.put(field.getName(), field.getValue());
                }
                contentStructure.put("fields", fieldMap);
            }
        }

        pkg.put("identityStatement", identityStatement);
        pkg.put("contextArea", contextArea);
        pkg.put("contentStructure", contentStructure);
        return pkg;
    }
}
