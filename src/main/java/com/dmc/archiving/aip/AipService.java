package com.dmc.archiving.aip;

import com.dmc.archiving.aip.input.CreateAipInput;
import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.aip.model.AipStatus;
import com.dmc.archiving.aip.generator.AipGenerator;
import com.dmc.archiving.aip.generator.AipGeneratorFactory;
import com.dmc.archiving.aip.repository.AipRepository;
import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.api.UserApi;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AipService {

    private final AipRepository aipRepository;
    private final UserApi userApi;
    private final AipGeneratorFactory aipGeneratorFactory;
    private final com.dmc.archiving.tenancy.api.PremiumOverageGuard premiumOverageGuard;

    public AipService(AipRepository aipRepository, UserApi userApi, AipGeneratorFactory aipGeneratorFactory,
                      com.dmc.archiving.tenancy.api.PremiumOverageGuard premiumOverageGuard) {
        this.aipRepository = aipRepository;
        this.userApi = userApi;
        this.aipGeneratorFactory = aipGeneratorFactory;
        this.premiumOverageGuard = premiumOverageGuard;
    }

    @Transactional
    public String generateAip(Long aipId) {
        Aip aip = aipRepository.findById(aipId)
                .orElseThrow(() -> new IllegalArgumentException("Aip not found: " + aipId));
        AipGenerator generator = aipGeneratorFactory.getGenerator(aip.getStandard());
        return generator.generate(aip);
    }

    @Transactional
    public Aip createAip(CreateAipInput input) {
        User user = userApi.getUserById(input.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + input.getUserId() + " does not exist"));

        Long ownerId = input.getOwnerId() != null ? input.getOwnerId() : input.getUserId();
        Long tenantId = input.getTenantId() != null ? input.getTenantId() : ownerId;

        // Premium-package spend cap, enforced inside this (write) transaction so
        // the count and the insert are atomic under a per-tenant lock.
        if (input.isBillable() && input.getStandard() != null
                && premiumOverageGuard.isPremiumStandard(input.getStandard().name())) {
            premiumOverageGuard.checkCanCreatePremiumPackage(tenantId);
        }

        LocalDateTime now = LocalDateTime.now();

        Aip aip = new Aip();
        aip.setTenantId(tenantId);
        aip.setOwnerId(ownerId);
        aip.setTitle(input.getTitle());
        aip.setDescription(input.getDescription());
        aip.setContent(input.getContent());
        aip.setCreatedAt(now);
        aip.setUpdatedAt(now);
        aip.setStatus(AipStatus.DRAFT);
        aip.setStandard(input.getStandard());
        aip.setSourceSipId(input.getSourceSipId());
        aip.setBillable(input.isBillable());

        // Assign creator
        aip.assignUser(user);

        // Create root element inline
        Element rootElement = getRootElement(input, now);

        // Add fields to root element
        if (input.getFields() != null && !input.getFields().isEmpty()) {
            for (Map<String, Object> fieldInput : input.getFields()) {
                Field field = new Field();
                field.setElement(rootElement);
                field.setName(fieldInput.get("name").toString());
                field.setLabel(fieldInput.get("label") != null ? fieldInput.get("label").toString() : null);
                field.setType(fieldInput.get("type").toString());
                field.setValue(fieldInput.get("value") != null ? fieldInput.get("value").toString() : null);
                rootElement.addField(field);
            }
        }

        aip.setRootElement(rootElement);

        return aipRepository.save(aip);
    }

    private static @NonNull Element getRootElement(CreateAipInput input, LocalDateTime now) {
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
        rootElement.setStatus("Opprettet");
        return rootElement;
    }

    public List<Aip> getAllAips() {
        return aipRepository.findAll();
    }

    public List<Aip> getAipsByTenant(Long tenantId) {
        return aipRepository.findByTenantId(tenantId);
    }

    public Aip getAip(Long id) {
        return aipRepository.findById(id).orElse(null);
    }

    @Transactional
    public Aip updateAipStatus(Long aipId, AipStatus status) {
        Aip aip = aipRepository.findById(aipId)
                .orElseThrow(() -> new IllegalArgumentException("Aip with ID " + aipId + " does not exist"));
        aip.setStatus(status);
        aip.setUpdatedAt(LocalDateTime.now());
        return aipRepository.save(aip);
    }

    @Transactional
    public boolean deleteAip(Long id) {
        if (!aipRepository.existsById(id)) {
            throw new IllegalArgumentException("Aip with ID " + id + " does not exist");
        }
        aipRepository.deleteById(id);
        return true;
    }

    /**
     * Count AIPs for a tenant whose standard is in the given set
     * (SQL aggregate, for usage metering).
     */
    public long countByTenantAndStandards(Long tenantId,
                                          java.util.Collection<com.dmc.archiving.archive.model.ArchiveStandard> standards) {
        return aipRepository.countByTenantIdAndStandardInAndBillableTrue(tenantId, standards);
    }

    /** Billable premium AIPs GENERATED in the half-open [start, end) — per-period billing. */
    public long countByTenantAndStandardsGeneratedIn(
            Long tenantId, java.util.Collection<com.dmc.archiving.archive.model.ArchiveStandard> standards,
            java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return aipRepository.countByTenantIdAndStandardInAndBillableTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                tenantId, standards, start, end);
    }
}
