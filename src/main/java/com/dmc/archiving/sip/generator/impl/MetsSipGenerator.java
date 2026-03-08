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
public class MetsSipGenerator extends AbstractSipGenerator {

    public MetsSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "METS";
    }

    @Override
    public Map<String, Object> buildPackage(Sip sip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "METS");
        pkg.put("sipId", sip.getId());
        pkg.put("status", sip.getStatus().name());
        pkg.put("createdAt", sip.getCreatedAtString());

        Map<String, Object> metsHeader = new LinkedHashMap<>();
        metsHeader.put("createDate", sip.getCreatedAtString());
        metsHeader.put("lastModDate", sip.getUpdatedAtString());
        metsHeader.put("recordStatus", sip.getStatus().name());

        Map<String, Object> dmdSec = new LinkedHashMap<>();
        dmdSec.put("title", sip.getTitle());
        dmdSec.put("description", sip.getDescription());

        Map<String, Object> fileSec = new LinkedHashMap<>();
        Map<String, Object> structMap = new LinkedHashMap<>();

        Element root = sip.getRootElement();
        if (root != null) {
            dmdSec.put("entityName", root.getEntityName());
            dmdSec.put("entityType", root.getEntityType());

            structMap.put("type", "logical");
            structMap.put("label", root.getTitle());
            structMap.put("elementIdentifier", root.getElementIdentifier());

            List<Field> fields = root.getFields();
            if (fields != null && !fields.isEmpty()) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                for (Field field : fields) {
                    fieldMap.put(field.getName(), field.getValue());
                }
                fileSec.put("fileGrp", fieldMap);
            }
        }

        pkg.put("mets:header", metsHeader);
        pkg.put("mets:dmdSec", dmdSec);
        pkg.put("mets:fileSec", fileSec);
        pkg.put("mets:structMap", structMap);
        return pkg;
    }
}
