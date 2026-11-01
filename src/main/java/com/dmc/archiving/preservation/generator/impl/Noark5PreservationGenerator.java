package com.dmc.archiving.preservation.generator.impl;

import com.dmc.archiving.pkg.generator.PackagePayload;
import com.dmc.archiving.pkg.generator.PackageSnapshot;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.ExtractInput;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.Stage;
import com.dmc.archiving.preservation.generator.AbstractPreservationGenerator;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

@Component
public class Noark5PreservationGenerator extends AbstractPreservationGenerator {

    public Noark5PreservationGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "NOARK5";
    }

    @Override
    protected PackagePayload buildPayload(PackageSnapshot s) {
        byte[] xml = Noark5XmlWriter.formatPretty(Noark5XmlWriter.write(new ExtractInput(
                Stage.AIP,
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

        return new PackagePayload(xml, Noark5XmlWriter.filename(Stage.AIP), "application/xml");
    }
}
