package com.dmc.archiving.pkg.repository;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.pkg.model.ArchivalPackage;
import com.dmc.archiving.pkg.model.PackageStage;
import com.dmc.archiving.pkg.model.PackageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageRepository extends JpaRepository<ArchivalPackage, Long> {

    List<ArchivalPackage> findByStageOrderByCreatedAtDesc(PackageStage stage);

    List<ArchivalPackage> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<ArchivalPackage> findByStageAndTenantIdOrderByCreatedAtDesc(PackageStage stage, Long tenantId);

    List<ArchivalPackage> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<ArchivalPackage> findByStageAndOwnerIdOrderByCreatedAtDesc(PackageStage stage, Long ownerId);

    List<ArchivalPackage> findBySourcePackageIdOrderByCreatedAtDesc(Long sourcePackageId);

    List<ArchivalPackage> findBySourceArchiveIdOrderByCreatedAtDesc(Long sourceArchiveId);

    long countByStage(PackageStage stage);

    long countByStageAndTenantId(PackageStage stage, Long tenantId);

    long countByStageAndStatus(PackageStage stage, PackageStatus status);

    List<ArchivalPackage> findAllByOrderByCreatedAtDesc();
}
