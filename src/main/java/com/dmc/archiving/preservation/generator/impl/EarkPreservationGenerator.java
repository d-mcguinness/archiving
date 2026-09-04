package com.dmc.archiving.preservation.generator.impl;

import com.dmc.archiving.preservation.generator.AbstractPreservationGenerator;
import com.dmc.archiving.preservation.model.Preservation;
import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EarkPreservationGenerator extends AbstractPreservationGenerator {

    public EarkPreservationGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "EARK";
    }

    @Override
    public Map<String, Object> buildPackage(Preservation aip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "EARK");
        pkg.put("standardVersion", "E-ARK CSIP 2.1.0");
        pkg.put("aipId", aip.getId());
        pkg.put("title", aip.getTitle());
        pkg.put("description", aip.getDescription());
        pkg.put("status", aip.getStatus().name());
        pkg.put("createdAt", aip.getCreatedAtString());

        // METS header
        Map<String, Object> metsHeader = new LinkedHashMap<>();
        metsHeader.put("packageID", "AIP-" + aip.getId());
        metsHeader.put("contentInformationType", "MIXED");
        metsHeader.put("profile", "https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml");
        metsHeader.put("oaisPackageType", "AIP");
        pkg.put("metsHeader", metsHeader);

        // Structural map
        Map<String, Object> structMap = new LinkedHashMap<>();
        structMap.put("type", "physical");
        structMap.put("label", "CSIP AIP Structure");

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

            structMap.put("rootElement", rootElement);
        }

        pkg.put("structMap", structMap);

        // Metadata sections
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("descriptive", Map.of("type", "EAD", "status", "current"));
        metadata.put("preservation", Map.of("type", "PREMIS", "status", "current"));
        metadata.put("administrative", Map.of("type", "PREMIS", "status", "current"));
        pkg.put("metadata", metadata);

        // Representations
        pkg.put("representations", List.of(
            Map.of("id", "rep-001", "type", "original", "label", "Original representation")
        ));

        // Schemas and documentation
        pkg.put("schemas", List.of("mets.xsd", "premis.xsd", "ead3.xsd"));
        pkg.put("documentation", List.of("README.md", "submission_agreement.pdf"));

        // Source SIP reference
        if (aip.getSourceIntakeId() != null) {
            pkg.put("sourceIntakeId", aip.getSourceIntakeId());
        }

        return pkg;
    }
}
