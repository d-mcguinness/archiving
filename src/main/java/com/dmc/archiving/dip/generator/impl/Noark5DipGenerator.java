package com.dmc.archiving.dip.generator.impl;

import com.dmc.archiving.dip.generator.AbstractDipGenerator;
import com.dmc.archiving.dip.model.Dip;
import com.dmc.archiving.pkg.generator.PackagePayload;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.ExtractInput;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.Stage;
import com.dmc.archiving.storage.CloudStorageService;
import org.springframework.stereotype.Component;

@Component
public class Noark5DipGenerator extends AbstractDipGenerator {

    public Noark5DipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String getStandardName() {
        return "NOARK5";
    }

    @Override
    protected PackagePayload buildPayload(Dip dip) {
        byte[] xml = Noark5XmlWriter.formatPretty(Noark5XmlWriter.write(new ExtractInput(
                Stage.DIP,
                dip.getId(),
                dip.getTitle(),
                dip.getDescription(),
                dip.getCreatedAtString(),
                dip.getUpdatedAtString(),
                dip.getRootElement() != null ? dip.getRootElement().getCreatedBy() : null,
                dip.getStatus() != null ? dip.getStatus().name() : null,
                dip.getRootElement(),
                dip.getSourceAipId()
        )));

        return new PackagePayload(xml, Noark5XmlWriter.filename(Stage.DIP), "application/xml");
    }
}
