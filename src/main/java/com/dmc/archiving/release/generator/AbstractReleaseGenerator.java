package com.dmc.archiving.release.generator;

import com.dmc.archiving.pkg.generator.AbstractPackageGenerator;
import com.dmc.archiving.pkg.generator.PackageSnapshot;
import com.dmc.archiving.release.model.Release;
import com.dmc.archiving.storage.CloudStorageService;

public abstract class AbstractReleaseGenerator
        extends AbstractPackageGenerator<Release>
        implements ReleaseGenerator {

    protected AbstractReleaseGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    public String packageType() {
        return "dip";
    }

    @Override
    protected PackageSnapshot toSnapshot(Release dip) {
        return PackageSnapshot.of(
                dip.getId(), dip.getTitle(), dip.getDescription(),
                dip.getStatus(), dip.getStandard(),
                dip.getCreatedAtString(), dip.getUpdatedAtString(),
                dip.getSourcePreservationId(),
                dip.getRootElement());
    }
}
