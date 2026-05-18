package com.dmc.archiving.sip.generator.impl;

import com.dmc.archiving.sip.generator.AbstractSipGenerator;
import com.dmc.archiving.sip.generator.SipSnapshot;
import com.dmc.archiving.sip.input.FileMetadataInput;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MetsSipGenerator extends AbstractSipGenerator {

    public MetsSipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "METS";
    }

    @Override
    public Map<String, Object> buildPackage(SipSnapshot s) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("standard", "METS");
        pkg.put("sipId", s.id());
        pkg.put("status", s.status());
        pkg.put("createdAt", s.createdAt());

        Map<String, Object> metsHeader = new LinkedHashMap<>();
        metsHeader.put("createDate", s.createdAt());
        metsHeader.put("lastModDate", s.updatedAt());
        metsHeader.put("recordStatus", s.status());

        Map<String, Object> dmdSec = new LinkedHashMap<>();
        dmdSec.put("title", s.title());
        dmdSec.put("description", s.description());

        Map<String, Object> fileSec = new LinkedHashMap<>();
        Map<String, Object> structMap = new LinkedHashMap<>();

        if (s.hasRootElement()) {
            dmdSec.put("entityName", s.entityName());
            dmdSec.put("entityType", s.entityType());

            structMap.put("type", "logical");
            structMap.put("label", s.elementTitle());
            structMap.put("elementIdentifier", s.elementIdentifier());

            if (s.hasFields()) {
                fileSec.put("fileGrp", s.fields());
            }
        }

        pkg.put("mets:header", metsHeader);
        pkg.put("mets:dmdSec", dmdSec);
        pkg.put("mets:fileSec", fileSec);
        pkg.put("mets:structMap", structMap);
        return pkg;
    }

    @Override
    public Map<String, String> prefillFields(FileMetadataInput meta) {
        PrefillContext c = prefillContext(meta);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("metsID", c.id());
        m.put("objID", "OBJ-" + c.id());
        m.put("label", c.name());
        m.put("type", "digital object");
        m.put("profile", "http://www.loc.gov/standards/mets/profiles");
        return m;
    }
}
