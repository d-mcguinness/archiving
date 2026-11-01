package com.dmc.archiving.preservation.generator;

import com.dmc.archiving.pkg.generator.AbstractPackageGenerator;
import com.dmc.archiving.pkg.generator.PackageSnapshot;
import com.dmc.archiving.preservation.model.Preservation;
import com.dmc.archiving.storage.CloudStorageService;

public abstract class AbstractPreservationGenerator
        extends AbstractPackageGenerator<Preservation>
        implements PreservationGenerator {

    protected AbstractPreservationGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String packageType() {
        return "aip";
    }

    @Override
    protected PackageSnapshot toSnapshot(Preservation aip) {
        return PackageSnapshot.of(
                aip.getId(), aip.getTitle(), aip.getDescription(),
                aip.getStatus(), aip.getStandard(),
                aip.getCreatedAtString(), aip.getUpdatedAtString(),
                aip.getSourceIntakeId(),
                aip.getRootElement());
    }
}
