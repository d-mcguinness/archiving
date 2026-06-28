package com.dmc.archiving.preservation;

import com.dmc.archiving.preservation.input.CreatePreservationInput;
import com.dmc.archiving.preservation.model.Preservation;
import com.dmc.archiving.preservation.model.PreservationStatus;
import com.dmc.archiving.preservation.generator.PreservationGenerator;
import com.dmc.archiving.preservation.generator.PreservationGeneratorFactory;
import com.dmc.archiving.preservation.repository.PreservationRepository;
import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.tenancy.api.PremiumPackageType;
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
public class PreservationService {

    private final PreservationRepository aipRepository;
    private final UserApi userApi;
    private final PreservationGeneratorFactory aipGeneratorFactory;
    private final com.dmc.archiving.tenancy.api.PremiumOverageGuard premiumOverageGuard;

    public PreservationService(PreservationRepository aipRepository, UserApi userApi, PreservationGeneratorFactory aipGeneratorFactory,
                      com.dmc.archiving.tenancy.api.PremiumOverageGuard premiumOverageGuard) {
        this.aipRepository = aipRepository;
        this.userApi = userApi;
        this.aipGeneratorFactory = aipGeneratorFactory;
        this.premiumOverageGuard = premiumOverageGuard;
    }

    @Transactional
    public String generatePreservation(Long preservationId) {
        Preservation aip = aipRepository.findById(preservationId)
                .orElseThrow(() -> new IllegalArgumentException("Preservation not found: " + preservationId));
        PreservationGenerator generator = aipGeneratorFactory.getGenerator(aip.getStandard());
        return generator.generate(aip);
    }

    @Transactional
    public Preservation createPreservation(CreatePreservationInput input) {
        User user = userApi.getUserById(input.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + input.getUserId() + " does not exist"));

        Long ownerId = input.getOwnerId() != null ? input.getOwnerId() : input.getUserId();
        if (input.getTenantId() == null) {
            // Billing attribution must be unambiguous: never fall back to the owner
            // as the billing tenant. The tenant is resolved before the package is
            // created, so a null here is a programming error, not a billable event.
            throw new IllegalArgumentException("createPreservation requires a resolved tenantId for billing attribution");
        }
        Long tenantId = input.getTenantId();

        // A billable premium-standard generation is metered: cap-checked before
        // the insert and recorded to the append-only ledger after — both inside
        // this (write) transaction, under a per-tenant lock, so the count and
        // the insert are atomic and a rollback drops the ledger event too.
        boolean meterPremium = input.isBillable() && input.getStandard() != null
                && premiumOverageGuard.isPremiumStandard(input.getStandard().name());
        if (meterPremium) {
            premiumOverageGuard.checkCanCreatePremiumPackage(tenantId);
        }

        LocalDateTime now = LocalDateTime.now();

        Preservation aip = new Preservation();
        aip.setTenantId(tenantId);
        aip.setOwnerId(ownerId);
        aip.setTitle(input.getTitle());
        aip.setDescription(input.getDescription());
        aip.setContent(input.getContent());
        aip.setCreatedAt(now);
        aip.setUpdatedAt(now);
        aip.setStatus(PreservationStatus.DRAFT);
        aip.setStandard(input.getStandard());
        aip.setSourceIntakeId(input.getSourceIntakeId());
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

        Preservation saved = aipRepository.save(aip);
        if (meterPremium) {
            premiumOverageGuard.recordPremiumPackageGenerated(
                    tenantId, input.getStandard().name(), PremiumPackageType.PRESERVATION);
        }
        return saved;
    }

    private static @NonNull Element getRootElement(CreatePreservationInput input, LocalDateTime now) {
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

    public List<Preservation> getAllPreservations() {
        return aipRepository.findAll();
    }

    public List<Preservation> getPreservationsByTenant(Long tenantId) {
        return aipRepository.findByTenantId(tenantId);
    }

    public Preservation getPreservation(Long id) {
        return aipRepository.findById(id).orElse(null);
    }

    @Transactional
    public Preservation updatePreservationStatus(Long preservationId, PreservationStatus status) {
        Preservation aip = aipRepository.findById(preservationId)
                .orElseThrow(() -> new IllegalArgumentException("Preservation with ID " + preservationId + " does not exist"));
        aip.setStatus(status);
        aip.setUpdatedAt(LocalDateTime.now());
        return aipRepository.save(aip);
    }

    @Transactional
    public boolean deletePreservation(Long id) {
        if (!aipRepository.existsById(id)) {
            throw new IllegalArgumentException("Preservation with ID " + id + " does not exist");
        }
        aipRepository.deleteById(id);
        return true;
    }
}
