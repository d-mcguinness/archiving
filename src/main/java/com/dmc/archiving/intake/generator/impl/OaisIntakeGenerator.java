package com.dmc.archiving.intake.generator.impl;

import com.dmc.archiving.intake.generator.AbstractIntakeGenerator;
import com.dmc.archiving.intake.generator.IntakeSnapshot;
import com.dmc.archiving.intake.input.FileMetadataInput;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OaisIntakeGenerator extends AbstractIntakeGenerator {

    public OaisIntakeGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "OAIS";
    }

    @Override
    public Map<String, Object> buildPackage(IntakeSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "OAIS");
        pkg.put("sipId", s.id());
        pkg.put("title", s.title());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        Map<String, Object> informationPackage = new LinkedHashMap<>();
        informationPackage.put("type", "SIP");
        informationPackage.put("title", s.title());
        informationPackage.put("description", s.description());

        Map<String, Object> contentInformation = new LinkedHashMap<>();
        Map<String, Object> preservationDescription = new LinkedHashMap<>();

        if (s.hasRootElement()) {
            contentInformation.put("entityName", s.entityName());
            contentInformation.put("entityType", s.entityType());
            contentInformation.put("title", s.elementTitle());

            if (s.hasFields()) {
                contentInformation.put("dataObjects", s.fields());
            }

            preservationDescription.put("createdAt", s.elementCreatedAt());
            preservationDescription.put("createdBy", s.elementCreatedBy());
            preservationDescription.put("status", s.elementStatus());
        }

        informationPackage.put("contentInformation", contentInformation);
        informationPackage.put("preservationDescription", preservationDescription);
        pkg.put("informationPackage", informationPackage);
        return pkg;
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("packageID", c.id());
        m.put("title", c.name());
        m.put("description", "");
        m.put("submissionDate", c.date());
        m.put("producer", c.user());
        m.put("producerContact", "");
        m.put("submissionAgreementRef", "SA-" + c.id());
        m.put("packageType", "SIP");
        m.put("contentInformationType", mapContentType(c.type()));
        m.put("completeness", "Complete");
        m.put("numberOfObjects", String.valueOf(c.count()));
        m.put("totalSize", c.size());
        return m;
    }
}
