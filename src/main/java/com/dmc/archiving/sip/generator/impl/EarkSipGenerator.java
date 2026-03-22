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
public class EarkSipGenerator extends AbstractSipGenerator {

    public EarkSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "EARK";
    }

    @Override
    public Map<String, Object> buildPackage(Sip sip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "EARK");
        pkg.put("standardVersion", "E-ARK CSIP 2.1.0");
        pkg.put("sipId", sip.getId());
        pkg.put("title", sip.getTitle());
        pkg.put("description", sip.getDescription());
        pkg.put("status", sip.getStatus().name());
        pkg.put("createdAt", sip.getCreatedAtString());

        Map<String, Object> metsHeader = new LinkedHashMap<>();
        metsHeader.put("packageID", "SIP-" + sip.getId());
        metsHeader.put("contentInformationType", "MIXED");
        metsHeader.put("profile", "https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml");
        metsHeader.put("oaisPackageType", "SIP");

        Map<String, Object> structMap = new LinkedHashMap<>();
        structMap.put("type", "physical");
        structMap.put("label", "CSIP");

        Element root = sip.getRootElement();
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

            structMap.put("rootElement", rootElement);
        }

        pkg.put("metsHeader", metsHeader);
        pkg.put("structMap", structMap);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("descriptive", Map.of("type", "EAD", "status", "current"));
        metadata.put("administrative", Map.of("type", "PREMIS", "status", "current"));
        pkg.put("metadata", metadata);

        pkg.put("representations", List.of(
            Map.of("id", "rep-001", "type", "original", "label", "Original representation")
        ));

        return pkg;
    }
}
