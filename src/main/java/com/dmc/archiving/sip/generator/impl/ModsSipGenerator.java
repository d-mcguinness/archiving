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
public class ModsSipGenerator extends AbstractSipGenerator {

    public ModsSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "MODS";
    }

    @Override
    public Map<String, Object> buildPackage(Sip sip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "MODS");
        pkg.put("sipId", sip.getId());
        pkg.put("status", sip.getStatus().name());
        pkg.put("createdAt", sip.getCreatedAtString());

        Map<String, Object> titleInfo = new LinkedHashMap<>();
        titleInfo.put("title", sip.getTitle());
        titleInfo.put("subTitle", sip.getDescription());

        Map<String, Object> name = new LinkedHashMap<>();
        Map<String, Object> originInfo = new LinkedHashMap<>();
        originInfo.put("dateCreated", sip.getCreatedAtString());

        Element root = sip.getRootElement();
        if (root != null) {
            name.put("namePart", root.getCreatedBy());
            name.put("role", "creator");

            originInfo.put("publisher", root.getEntityName());

            Map<String, Object> subject = new LinkedHashMap<>();
            subject.put("entityType", root.getEntityType());
            subject.put("identifier", root.getElementIdentifier());
            pkg.put("mods:subject", subject);

            List<Field> fields = root.getFields();
            if (fields != null && !fields.isEmpty()) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                for (Field field : fields) {
                    fieldMap.put(field.getName(), field.getValue());
                }
                pkg.put("mods:extension", fieldMap);
            }
        }

        pkg.put("mods:titleInfo", titleInfo);
        pkg.put("mods:name", name);
        pkg.put("mods:originInfo", originInfo);
        return pkg;
    }
}
