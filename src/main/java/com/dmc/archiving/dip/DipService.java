package com.dmc.archiving.dip;

import com.dmc.archiving.dip.input.CreateDipInput;
import com.dmc.archiving.dip.model.Dip;
import com.dmc.archiving.dip.model.DipStatus;
import com.dmc.archiving.dip.generator.DipGenerator;
import com.dmc.archiving.dip.generator.DipGeneratorFactory;
import com.dmc.archiving.dip.repository.DipRepository;
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
public class DipService {

    private final DipRepository dipRepository;
    private final UserApi userApi;
    private final DipGeneratorFactory dipGeneratorFactory;

    public DipService(DipRepository dipRepository, UserApi userApi, DipGeneratorFactory dipGeneratorFactory) {
        this.dipRepository = dipRepository;
        this.userApi = userApi;
        this.dipGeneratorFactory = dipGeneratorFactory;
    }

    @Transactional
    public String generateDip(Long dipId) {
        Dip dip = dipRepository.findById(dipId)
                .orElseThrow(() -> new IllegalArgumentException("Dip not found: " + dipId));
        DipGenerator generator = dipGeneratorFactory.getGenerator(dip.getStandard());
        return generator.generate(dip);
    }

    @Transactional
    public Dip createDip(CreateDipInput input) {
        User user = userApi.getUserById(input.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + input.getUserId() + " does not exist"));

        Long ownerId = input.getOwnerId() != null ? input.getOwnerId() : input.getUserId();
        Long tenantId = input.getTenantId() != null ? input.getTenantId() : ownerId;

        LocalDateTime now = LocalDateTime.now();

        Dip dip = new Dip();
        dip.setTenantId(tenantId);
        dip.setOwnerId(ownerId);
        dip.setTitle(input.getTitle());
        dip.setDescription(input.getDescription());
        dip.setContent(input.getContent());
        dip.setCreatedAt(now);
        dip.setUpdatedAt(now);
        dip.setStatus(DipStatus.DRAFT);
        dip.setStandard(input.getStandard());
        dip.setSourceAipId(input.getSourceAipId());
        dip.setBillable(input.isBillable());

        // Assign creator
        dip.assignUser(user);

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

        dip.setRootElement(rootElement);

        return dipRepository.save(dip);
    }

    private static @NonNull Element getRootElement(CreateDipInput input, LocalDateTime now) {
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

    public List<Dip> getAllDips() {
        return dipRepository.findAll();
    }

    public List<Dip> getDipsByTenant(Long tenantId) {
        return dipRepository.findByTenantId(tenantId);
    }

    public Dip getDip(Long id) {
        return dipRepository.findById(id).orElse(null);
    }

    @Transactional
    public Dip updateDipStatus(Long dipId, DipStatus status) {
        Dip dip = dipRepository.findById(dipId)
                .orElseThrow(() -> new IllegalArgumentException("Dip with ID " + dipId + " does not exist"));
        dip.setStatus(status);
        dip.setUpdatedAt(LocalDateTime.now());
        return dipRepository.save(dip);
    }

    @Transactional
    public boolean deleteDip(Long id) {
        if (!dipRepository.existsById(id)) {
            throw new IllegalArgumentException("Dip with ID " + id + " does not exist");
        }
        dipRepository.deleteById(id);
        return true;
    }

    /**
     * Count DIPs for a tenant whose standard is in the given set
     * (SQL aggregate, for usage metering).
     */
    public long countByTenantAndStandards(Long tenantId,
                                          java.util.Collection<com.dmc.archiving.archive.model.ArchiveStandard> standards) {
        return dipRepository.countByTenantIdAndStandardInAndBillableTrue(tenantId, standards);
    }

    /** Billable premium DIPs GENERATED in the half-open [start, end) — per-period billing. */
    public long countByTenantAndStandardsGeneratedIn(
            Long tenantId, java.util.Collection<com.dmc.archiving.archive.model.ArchiveStandard> standards,
            java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return dipRepository.countByTenantIdAndStandardInAndBillableTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                tenantId, standards, start, end);
    }
}
