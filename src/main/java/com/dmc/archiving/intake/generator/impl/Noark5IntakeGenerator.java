package com.dmc.archiving.intake.generator.impl;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.pkg.generator.PackagePayload;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.ExtractInput;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.Stage;
import com.dmc.archiving.intake.generator.AbstractIntakeGenerator;
import com.dmc.archiving.intake.generator.IntakeSnapshot;
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
    protected PackagePayload buildPayload(IntakeSnapshot s) {
        // Reconstruct the Element from the snapshot (the writer reads getFields/etc.)
        Element root = rebuildElement(s);

        byte[] xml = Noark5XmlWriter.formatPretty(Noark5XmlWriter.write(new ExtractInput(
                Stage.SIP,
                s.id(),
                s.title(),
                s.description(),
                s.createdAt(),
                s.updatedAt(),
                s.elementCreatedBy(),
                s.status(),
                root,
                null
        )));

        return new PackagePayload(xml, Noark5XmlWriter.filename(Stage.SIP), "application/xml");
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("systemID", c.id());
        m.put("tittel", c.name());
        m.put("beskrivelse", "");
        m.put("arkivstatus", "Opprettet");
        m.put("dokumentmedium", "Elektronisk arkiv");
        m.put("opprettetDato", c.date());
        m.put("opprettetAv", c.user());
        m.put("avsluttetDato", c.date());
        m.put("avsluttetAv", c.user());
        return m;
    }

    private static Element rebuildElement(IntakeSnapshot s) {
        if (!s.hasRootElement()) return null;
        Element e = new Element();
        e.setElementIdentifier(s.elementIdentifier());
        e.setEntityName(s.entityName());
        e.setEntityType(s.entityType());
        e.setNorwegianName(s.norwegianName());
        e.setEnglishName(s.englishName());
        e.setTitle(s.elementTitle());
        e.setDescription(s.elementDescription());
        e.setCreatedBy(s.elementCreatedBy());
        e.setStatus(s.elementStatus());
        // Fields: IntakeSnapshot stores name→value strings; we don't reconstruct
        // Field entities here — the writer falls back to listing element fields
        // only if present. For SIP we keep it light; richer mapping can come
        // when we have the Field carrier in the snapshot.
        return e;
    }
}
