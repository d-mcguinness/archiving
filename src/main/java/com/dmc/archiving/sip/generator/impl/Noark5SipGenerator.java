package com.dmc.archiving.sip.generator.impl;

import com.dmc.archiving.sip.generator.AbstractSipGenerator;
import com.dmc.archiving.sip.generator.SipSnapshot;
import com.dmc.archiving.sip.input.FileMetadataInput;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
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
    public Map<String, Object> buildPackage(SipSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "NOARK5");
        pkg.put("standardVersion", "5.0");
        pkg.put("sipId", s.id());
        pkg.put("title", s.title());
        pkg.put("description", s.description());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        Map<String, Object> arkivdel = new LinkedHashMap<>();
        arkivdel.put("systemID", s.id().toString());
        arkivdel.put("tittel", s.title());
        arkivdel.put("beskrivelse", s.description() != null ? s.description() : "");
        arkivdel.put("dokumentmedium", "Elektronisk arkiv");

        if (s.hasRootElement()) {
            arkivdel.put("elementIdentifier", s.elementIdentifier());
            arkivdel.put("entityName", s.entityName());
            arkivdel.put("entityType", s.entityType());
            arkivdel.put("norwegianName", s.norwegianName());
            arkivdel.put("englishName", s.englishName());

            if (s.hasFields()) {
                arkivdel.put("fields", s.fields());
            }
        }

        pkg.put("arkivdel", arkivdel);
        return pkg;
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("systemID", c.id());
        m.put("title", c.name());
        m.put("description", "");
        m.put("archiveStatus", "Created");
        m.put("documentMedium", "Electronic archive");
        m.put("storageLocation", "Default storage");
        m.put("createdDate", c.date());
        m.put("createdBy", c.user());
        m.put("closedDate", c.date());
        m.put("closedBy", c.user());
        return m;
    }
}
