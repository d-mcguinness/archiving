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
public class BagitSipGenerator extends AbstractSipGenerator {

    public BagitSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "BAGIT";
    }

    @Override
    public Map<String, Object> buildPackage(Sip sip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "BAGIT");
        pkg.put("sipId", sip.getId());
        pkg.put("status", sip.getStatus().name());
        pkg.put("createdAt", sip.getCreatedAtString());

        Map<String, Object> bagitInfo = new LinkedHashMap<>();
        bagitInfo.put("BagIt-Version", "1.0");
        bagitInfo.put("Tag-File-Character-Encoding", "UTF-8");
        bagitInfo.put("Source-Organization", sip.getTitle());
        bagitInfo.put("Bagging-Date", sip.getCreatedAtString());
        bagitInfo.put("Bag-Count", "1 of 1");

        Map<String, Object> manifest = new LinkedHashMap<>();
        Map<String, Object> tagManifest = new LinkedHashMap<>();
        Map<String, Object> dataReferences = new LinkedHashMap<>();

        Element root = sip.getRootElement();
        if (root != null) {
            dataReferences.put("entityName", root.getEntityName());
            dataReferences.put("entityType", root.getEntityType());
            dataReferences.put("title", root.getTitle());
            dataReferences.put("identifier", root.getElementIdentifier());

            List<Field> fields = root.getFields();
            if (fields != null && !fields.isEmpty()) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                for (Field field : fields) {
                    fieldMap.put(field.getName(), field.getValue());
                }
                dataReferences.put("fields", fieldMap);
            }
        }

        pkg.put("bagit-info", bagitInfo);
        pkg.put("manifest", manifest);
        pkg.put("tag-manifest", tagManifest);
        pkg.put("data", dataReferences);
        return pkg;
    }
}
