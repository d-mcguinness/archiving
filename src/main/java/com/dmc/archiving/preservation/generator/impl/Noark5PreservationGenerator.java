package com.dmc.archiving.preservation.generator.impl;

import com.dmc.archiving.preservation.generator.AbstractPreservationGenerator;
import com.dmc.archiving.preservation.model.Preservation;
import com.dmc.archiving.pkg.generator.PackagePayload;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.ExtractInput;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.Stage;
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
    protected PackagePayload buildPayload(Preservation aip) {
        byte[] xml = Noark5XmlWriter.formatPretty(Noark5XmlWriter.write(new ExtractInput(
                Stage.AIP,
                aip.getId(),
                aip.getTitle(),
                aip.getDescription(),
                aip.getCreatedAtString(),
                aip.getUpdatedAtString(),
                aip.getRootElement() != null ? aip.getRootElement().getCreatedBy() : null,
                aip.getStatus() != null ? aip.getStatus().name() : null,
                aip.getRootElement(),
                aip.getSourceIntakeId()
        )));

        return new PackagePayload(xml, Noark5XmlWriter.filename(Stage.AIP), "application/xml");
    }
}
