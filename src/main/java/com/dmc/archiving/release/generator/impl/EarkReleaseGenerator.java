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
public class EarkReleaseGenerator extends AbstractReleaseGenerator {

    public EarkReleaseGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "EARK";
    }

    @Override
    public Map<String, Object> buildPackage(Release dip) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "EARK");
        pkg.put("standardVersion", "E-ARK CSIP 2.1.0");
        pkg.put("dipId", dip.getId());
        pkg.put("title", dip.getTitle());
        pkg.put("description", dip.getDescription());
        pkg.put("status", dip.getStatus().name());
        pkg.put("createdAt", dip.getCreatedAtString());

        // METS header
        Map<String, Object> metsHeader = new LinkedHashMap<>();
        metsHeader.put("packageID", "DIP-" + dip.getId());
        metsHeader.put("contentInformationType", "MIXED");
        metsHeader.put("profile", "https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml");
        metsHeader.put("oaisPackageType", "DIP");
        pkg.put("metsHeader", metsHeader);

        // Structural map
        Map<String, Object> structMap = new LinkedHashMap<>();
        structMap.put("type", "physical");
        structMap.put("label", "CSIP DIP Structure");

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

            structMap.put("rootElement", rootElement);
        }

        pkg.put("structMap", structMap);

        // Metadata sections
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("descriptive", Map.of("type", "EAD", "status", "current"));
        metadata.put("preservation", Map.of("type", "PREMIS", "status", "current"));
        metadata.put("administrative", Map.of("type", "PREMIS", "status", "current"));
        pkg.put("metadata", metadata);

        // Dissemination section
        Map<String, Object> dissemination = new LinkedHashMap<>();
        dissemination.put("accessRights", "Public");
        dissemination.put("preservationLevel", "reference");
        dissemination.put("distributionFormat", "application/json");
        pkg.put("dissemination", dissemination);

        // Representations
        pkg.put("representations", List.of(
            Map.of("id", "rep-001", "type", "access", "label", "Access representation")
        ));

        // Schemas and documentation
        pkg.put("schemas", List.of("mets.xsd", "premis.xsd", "ead3.xsd"));
        pkg.put("documentation", List.of("README.md", "dissemination_agreement.pdf"));

        // Source AIP reference
        if (dip.getSourcePreservationId() != null) {
            pkg.put("sourcePreservationId", dip.getSourcePreservationId());
        }

        return pkg;
    }
}
