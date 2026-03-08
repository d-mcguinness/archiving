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
public class EadSipGenerator extends AbstractSipGenerator {

    public EadSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "EAD";
    }

    @Override
    public Map<String, Object> buildPackage(Sip sip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "EAD");
        pkg.put("sipId", sip.getId());
        pkg.put("status", sip.getStatus().name());
        pkg.put("createdAt", sip.getCreatedAtString());

        Map<String, Object> eadHeader = new LinkedHashMap<>();
        eadHeader.put("eadid", sip.getId().toString());
        eadHeader.put("titleproper", sip.getTitle());

        Map<String, Object> archdesc = new LinkedHashMap<>();
        archdesc.put("level", "collection");

        Map<String, Object> did = new LinkedHashMap<>();
        did.put("unittitle", sip.getTitle());
        did.put("unitdate", sip.getCreatedAtString());
        did.put("abstract", sip.getDescription());

        Element root = sip.getRootElement();
        if (root != null) {
            did.put("unitid", root.getElementIdentifier());
            did.put("origination", root.getCreatedBy());
            archdesc.put("entityName", root.getEntityName());
            archdesc.put("entityType", root.getEntityType());

            List<Field> fields = root.getFields();
            if (fields != null && !fields.isEmpty()) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                for (Field field : fields) {
                    fieldMap.put(field.getName(), field.getValue());
                }
                did.put("fields", fieldMap);
            }
        }

        pkg.put("ead:header", eadHeader);
        pkg.put("ead:archdesc", archdesc);
        pkg.put("ead:did", did);
        return pkg;
    }
}
