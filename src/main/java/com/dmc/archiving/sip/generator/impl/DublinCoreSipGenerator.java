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
public class DublinCoreSipGenerator extends AbstractSipGenerator {

    public DublinCoreSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "DUBLIN_CORE";
    }

    @Override
    public Map<String, Object> buildPackage(Sip sip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "DUBLIN_CORE");
        pkg.put("sipId", sip.getId());
        pkg.put("status", sip.getStatus().name());
        pkg.put("createdAt", sip.getCreatedAtString());

        pkg.put("dc:title", sip.getTitle());
        pkg.put("dc:description", sip.getDescription());
        pkg.put("dc:date", sip.getCreatedAtString());

        Element root = sip.getRootElement();
        if (root != null) {
            pkg.put("dc:creator", root.getCreatedBy());
            pkg.put("dc:type", root.getEntityType());
            pkg.put("dc:identifier", root.getElementIdentifier());

            List<Field> fields = root.getFields();
            if (fields != null && !fields.isEmpty()) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                for (Field field : fields) {
                    fieldMap.put(field.getName(), field.getValue());
                }
                pkg.put("dc:elements", fieldMap);
            }
        }

        return pkg;
    }
}
