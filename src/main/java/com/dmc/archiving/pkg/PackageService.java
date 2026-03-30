package com.dmc.archiving.pkg;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.pkg.input.CreatePackageInput;
import com.dmc.archiving.pkg.model.ArchivalPackage;
import com.dmc.archiving.pkg.model.PackageStage;
import com.dmc.archiving.pkg.model.PackageStatus;
import com.dmc.archiving.pkg.repository.PackageRepository;
import com.dmc.archiving.user.api.UserApi;
import com.dmc.archiving.user.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PackageService {

    private final PackageRepository packageRepository;
    private final UserApi userApi;
    private final EntityManager entityManager;

    public PackageService(PackageRepository packageRepository, UserApi userApi, EntityManager entityManager) {
        this.packageRepository = packageRepository;
        this.userApi = userApi;
        this.entityManager = entityManager;
    }

    @Transactional
    public ArchivalPackage createPackage(CreatePackageInput input) {
        Long userId = Long.parseLong(input.getUserId());
        User user = userApi.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Long ownerId = input.getOwnerId() != null ? Long.parseLong(input.getOwnerId()) : userId;
        Long tenantId = input.getTenantId() != null ? Long.parseLong(input.getTenantId()) : ownerId;

        LocalDateTime now = LocalDateTime.now();

        ArchivalPackage pkg = new ArchivalPackage();
        pkg.setTenantId(tenantId);
        pkg.setOwnerId(ownerId);
        pkg.setTitle(input.getTitle());
        pkg.setDescription(input.getDescription());
        pkg.setContent(input.getContent());
        pkg.setCreatedAt(now);
        pkg.setUpdatedAt(now);
        pkg.setStage(PackageStage.valueOf(input.getStage()));
        pkg.setStatus(PackageStatus.DRAFT);
        pkg.setStandard(ArchiveStandard.valueOf(input.getStandard()));

        if (input.getSourceArchiveId() != null) {
            pkg.setSourceArchiveId(Long.parseLong(input.getSourceArchiveId()));
        }
        if (input.getSourcePackageId() != null) {
            pkg.setSourcePackageId(Long.parseLong(input.getSourcePackageId()));
        }

        // Assign creator
        pkg.assignUser(user);

        // Create root element
        Element rootElement = new Element();
        rootElement.setElementIdentifier(input.getElementIdentifier());
        rootElement.setEntityName(input.getEntityName());
        rootElement.setEntityType(input.getEntityType());
        rootElement.setNorwegianName(input.getNorwegianName());
        rootElement.setEnglishName(input.getEnglishName());
        rootElement.setTitle(input.getElementTitle());
        rootElement.setDescription(input.getElementDescription());
        rootElement.setCreatedBy(input.getCreatedBy());
        rootElement.setCreatedAt(now);
        rootElement.setIsRoot(true);
        rootElement.setStatus("Created");

        // Add fields
        if (input.getFields() != null) {
            for (CreatePackageInput.FieldInput fi : input.getFields()) {
                Field field = new Field();
                field.setElement(rootElement);
                field.setName(fi.getName());
                field.setLabel(fi.getLabel());
                field.setType(fi.getType());
                field.setValue(fi.getValue());
                rootElement.addField(field);
            }
        }

        pkg.setRootElement(rootElement);

        return packageRepository.save(pkg);
    }

    public List<ArchivalPackage> getAllPackages() {
        return packageRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ArchivalPackage> getPackagesByStage(PackageStage stage) {
        return packageRepository.findByStageOrderByCreatedAtDesc(stage);
    }

    public List<ArchivalPackage> getPackagesByTenant(Long tenantId) {
        return packageRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public List<ArchivalPackage> getPackagesByStageAndTenant(PackageStage stage, Long tenantId) {
        return packageRepository.findByStageAndTenantIdOrderByCreatedAtDesc(stage, tenantId);
    }

    public ArchivalPackage getPackage(Long id) {
        return packageRepository.findById(id).orElse(null);
    }

    @Transactional
    public ArchivalPackage updatePackageStatus(Long id, PackageStatus status) {
        ArchivalPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Package not found: " + id));
        pkg.setStatus(status);
        pkg.setUpdatedAt(LocalDateTime.now());
        return packageRepository.save(pkg);
    }

    @Transactional
    public boolean deletePackage(Long id) {
        ArchivalPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Package not found: " + id));

        entityManager.createNativeQuery("DELETE FROM package_users WHERE package_id = :pkgId")
                .setParameter("pkgId", id).executeUpdate();

        pkg.setRootElement(null);
        packageRepository.saveAndFlush(pkg);
        packageRepository.delete(pkg);
        return true;
    }

    public long countByStage(PackageStage stage) {
        return packageRepository.countByStage(stage);
    }

    public long countByStageAndTenant(PackageStage stage, Long tenantId) {
        return packageRepository.countByStageAndTenantId(stage, tenantId);
    }
}
