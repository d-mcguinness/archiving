package com.dmc.archiving.intake.generator.impl;

import com.dmc.archiving.pkg.generator.PackagePayload;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.ExtractInput;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.Stage;
import com.dmc.archiving.intake.generator.AbstractIntakeGenerator;
import com.dmc.archiving.pkg.generator.PackageSnapshot;
import com.dmc.archiving.intake.input.FileMetadataInput;
import com.dmc.archiving.intake.model.Intake;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class Noark5IntakeGenerator extends AbstractIntakeGenerator {

    public Noark5IntakeGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "NOARK5";
    }

    @Override
    protected PackagePayload buildPayload(PackageSnapshot s) {
        byte[] xml = Noark5XmlWriter.formatPretty(Noark5XmlWriter.write(new ExtractInput(
                Stage.SIP,
                s.id(),
                s.title(),
                s.description(),
                s.createdAt(),
                s.updatedAt(),
                s.elementCreatedBy(),
                s.status(),
                s.toElement(),
                s.sourceId()
        )));

        return new PackagePayload(xml, Noark5XmlWriter.filename(Stage.SIP), "application/xml");
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        Map<String, String> m = new LinkedHashMap<>();
        // Keys are the Archive entity's field names in schemes/noark5.json — the
        // form filters prefill results against that list, so the Norwegian
        // element names (tittel, opprettetDato, …) belong in the XML writer only.
        m.put("systemID", c.id());
        m.put("title", c.name());
        m.put("description", "");
        m.put("archiveStatus", "Created");
        m.put("documentMedium", "Electronic archive");
        m.put("createdDate", c.date());
        m.put("createdBy", c.user());
        m.put("closedDate", c.date());
        m.put("closedBy", c.user());
        return m;
    }
}
