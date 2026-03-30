package com.dmc.archiving.pkg;

import com.dmc.archiving.pkg.input.CreatePackageInput;
import com.dmc.archiving.pkg.model.ArchivalPackage;
import com.dmc.archiving.pkg.model.PackageStage;
import com.dmc.archiving.pkg.model.PackageStatus;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class PackageController {

    private final PackageService packageService;

    public PackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @QueryMapping
    public List<ArchivalPackage> getAllPackages(@Argument String stage) {
        if (stage != null) {
            return packageService.getPackagesByStage(PackageStage.valueOf(stage));
        }
        return packageService.getAllPackages();
    }

    @QueryMapping
    public List<ArchivalPackage> getPackagesByTenant(@Argument String tenantId, @Argument String stage) {
        Long tid = Long.parseLong(tenantId);
        if (stage != null) {
            return packageService.getPackagesByStageAndTenant(PackageStage.valueOf(stage), tid);
        }
        return packageService.getPackagesByTenant(tid);
    }

    @QueryMapping
    public ArchivalPackage getPackage(@Argument String id) {
        return packageService.getPackage(Long.parseLong(id));
    }

    @MutationMapping
    public ArchivalPackage createPackage(@Argument CreatePackageInput input) {
        return packageService.createPackage(input);
    }

    @MutationMapping
    public ArchivalPackage updatePackageStatus(@Argument String packageId, @Argument String status) {
        return packageService.updatePackageStatus(
                Long.parseLong(packageId),
                PackageStatus.valueOf(status)
        );
    }

    @MutationMapping
    public boolean deletePackage(@Argument String id) {
        return packageService.deletePackage(Long.parseLong(id));
    }

    // Schema mappings for ArchivalPackage type
    @SchemaMapping(typeName = "ArchivalPackage", field = "createdAt")
    public String createdAt(ArchivalPackage pkg) {
        return pkg.getCreatedAtString();
    }

    @SchemaMapping(typeName = "ArchivalPackage", field = "updatedAt")
    public String updatedAt(ArchivalPackage pkg) {
        return pkg.getUpdatedAtString();
    }

    @SchemaMapping(typeName = "ArchivalPackage", field = "ownerId")
    public String ownerId(ArchivalPackage pkg) {
        return pkg.getOwnerId() != null ? pkg.getOwnerId().toString() : null;
    }

    @SchemaMapping(typeName = "ArchivalPackage", field = "tenantId")
    public String tenantId(ArchivalPackage pkg) {
        return pkg.getTenantId() != null ? pkg.getTenantId().toString() : null;
    }

    @SchemaMapping(typeName = "ArchivalPackage", field = "sourceArchiveId")
    public String sourceArchiveId(ArchivalPackage pkg) {
        return pkg.getSourceArchiveId() != null ? pkg.getSourceArchiveId().toString() : null;
    }

    @SchemaMapping(typeName = "ArchivalPackage", field = "sourcePackageId")
    public String sourcePackageId(ArchivalPackage pkg) {
        return pkg.getSourcePackageId() != null ? pkg.getSourcePackageId().toString() : null;
    }
}
