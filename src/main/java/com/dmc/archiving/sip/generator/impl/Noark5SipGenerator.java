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
public class Noark5SipGenerator extends AbstractSipGenerator {

    public Noark5SipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "NOARK5";
    }

    @Override
    public Map<String, Object> buildPackage(Sip sip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "NOARK5");
        pkg.put("standardVersion", "5.0");
        pkg.put("sipId", sip.getId());
        pkg.put("title", sip.getTitle());
        pkg.put("description", sip.getDescription());
        pkg.put("status", sip.getStatus().name());
        pkg.put("createdAt", sip.getCreatedAtString());

        Map<String, Object> arkivdel = new LinkedHashMap<>();
        arkivdel.put("systemID", sip.getId().toString());
        arkivdel.put("tittel", sip.getTitle());
        arkivdel.put("beskrivelse", sip.getDescription() != null ? sip.getDescription() : "");
        arkivdel.put("dokumentmedium", "Elektronisk arkiv");

        Element root = sip.getRootElement();
        if (root != null) {
            arkivdel.put("elementIdentifier", root.getElementIdentifier());
            arkivdel.put("entityName", root.getEntityName());
            arkivdel.put("entityType", root.getEntityType());
            arkivdel.put("norwegianName", root.getNorwegianName());
            arkivdel.put("englishName", root.getEnglishName());

            List<Field> fields = root.getFields();
            if (fields != null && !fields.isEmpty()) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                for (Field field : fields) {
                    fieldMap.put(field.getName(), field.getValue());
                }
                arkivdel.put("fields", fieldMap);
            }
        }

        pkg.put("arkivdel", arkivdel);
        return pkg;
    }
}
