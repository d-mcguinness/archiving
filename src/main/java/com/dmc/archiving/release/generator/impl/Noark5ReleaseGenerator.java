package com.dmc.archiving.release.generator.impl;

import com.dmc.archiving.pkg.generator.PackagePayload;
import com.dmc.archiving.pkg.generator.PackageSnapshot;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.ExtractInput;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.Stage;
import com.dmc.archiving.release.generator.AbstractReleaseGenerator;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

@Component
public class Noark5ReleaseGenerator extends AbstractReleaseGenerator {

    public Noark5ReleaseGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "NOARK5";
    }

    @Override
    protected PackagePayload buildPayload(PackageSnapshot s) {
        byte[] xml = Noark5XmlWriter.formatPretty(Noark5XmlWriter.write(new ExtractInput(
                Stage.DIP,
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

        return new PackagePayload(xml, Noark5XmlWriter.filename(Stage.DIP), "application/xml");
    }
}
