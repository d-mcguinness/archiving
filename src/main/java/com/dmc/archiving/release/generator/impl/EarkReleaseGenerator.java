package com.dmc.archiving.release.generator.impl;

import com.dmc.archiving.release.generator.AbstractReleaseGenerator;
import com.dmc.archiving.pkg.generator.PackageSnapshot;
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
    public Map<String, Object> buildPackage(PackageSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "EARK");
        pkg.put("standardVersion", "E-ARK CSIP 2.1.0");
        pkg.put("dipId", s.id());
        pkg.put("title", s.title());
        pkg.put("description", s.description());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        // METS header
        Map<String, Object> metsHeader = new LinkedHashMap<>();
        metsHeader.put("packageID", "DIP-" + s.id());
        metsHeader.put("contentInformationType", "MIXED");
        metsHeader.put("profile", "https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml");
        metsHeader.put("oaisPackageType", "DIP");
        pkg.put("metsHeader", metsHeader);

        // Structural map
        Map<String, Object> structMap = new LinkedHashMap<>();
        structMap.put("type", "physical");
        structMap.put("label", "CSIP DIP Structure");

        if (s.hasRootElement()) {
            Map<String, Object> rootElement = new LinkedHashMap<>();
            rootElement.put("elementIdentifier", s.elementIdentifier());
            rootElement.put("entityName", s.entityName());
            rootElement.put("entityType", s.entityType());
            rootElement.put("title", s.elementTitle());
            rootElement.put("description", s.elementDescription());

            if (s.hasFields()) {
                rootElement.put("fields", s.fields());
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
        if (s.sourceId() != null) {
            pkg.put("sourcePreservationId", s.sourceId());
        }

        return pkg;
    }
}
