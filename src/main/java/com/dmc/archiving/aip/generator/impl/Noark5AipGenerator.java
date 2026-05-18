package com.dmc.archiving.aip.generator.impl;

import com.dmc.archiving.aip.generator.AbstractAipGenerator;
import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.pkg.generator.PackagePayload;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.ExtractInput;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.Stage;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

@Component
public class Noark5AipGenerator extends AbstractAipGenerator {

    public Noark5AipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "NOARK5";
    }

    @Override
    protected PackagePayload buildPayload(Aip aip) {
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
                aip.getSourceSipId()
        )));

        return new PackagePayload(xml, Noark5XmlWriter.filename(Stage.AIP), "application/xml");
    }
}
