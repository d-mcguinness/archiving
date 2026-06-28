package com.dmc.archiving.intake;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.intake.input.CreateIntakeInput;
import com.dmc.archiving.user.model.User;
import com.dmc.archiving.intake.model.Intake;
import com.dmc.archiving.intake.model.IntakeStatus;
import com.dmc.archiving.intake.generator.IntakeGenerator;
import com.dmc.archiving.intake.generator.IntakeGeneratorFactory;
import com.dmc.archiving.intake.repository.IntakeRepository;
import com.dmc.archiving.user.api.UserApi;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class IntakeService {

    private final IntakeRepository sipRepository;
    private final UserApi userApi;
    private final IntakeGeneratorFactory sipGeneratorFactory;
    private final EntityManager entityManager;

    public IntakeService(IntakeRepository sipRepository, UserApi userApi, IntakeGeneratorFactory sipGeneratorFactory, EntityManager entityManager) {
        this.sipRepository = sipRepository;
        this.userApi = userApi;
        this.sipGeneratorFactory = sipGeneratorFactory;
        this.entityManager = entityManager;
    }

    @Transactional
    public String generateIntake(Long intakeId) {
        Intake sip = sipRepository.findById(intakeId)
                .orElseThrow(() -> new IllegalArgumentException("Intake not found: " + intakeId));
        IntakeGenerator generator = sipGeneratorFactory.getGenerator(sip.getStandard());
        return generator.generate(sip);
    }

    @Transactional
    public Intake createIntake(CreateIntakeInput input) {
        User user = userApi.getUserById(input.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + input.getUserId() + " does not exist"));

        Long ownerId = input.getOwnerId() != null ? input.getOwnerId() : input.getUserId();
        Long tenantId = input.getTenantId() != null ? input.getTenantId() : ownerId;

        LocalDateTime now = LocalDateTime.now();

        Intake sip = new Intake();
        sip.setTenantId(tenantId);
        sip.setOwnerId(ownerId);
        sip.setArchiveId(input.getArchiveId());
        sip.setTitle(input.getTitle());
        sip.setDescription(input.getDescription());
        sip.setContent(input.getContent());
        sip.setCreatedAt(now);
        sip.setUpdatedAt(now);
        sip.setStatus(IntakeStatus.DRAFT);
        sip.setStandard(input.getStandard());

        // Assign creator
        sip.assignUser(user);

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

        sip.setRootElement(rootElement);

        return sipRepository.save(sip);
    }

    private static @NonNull Element getRootElement(CreateIntakeInput input, LocalDateTime now) {
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

    public List<Intake> getAllIntakes() {
        return sipRepository.findAll();
    }

    public List<Intake> getIntakesByTenant(Long tenantId) {
        return sipRepository.findByTenantId(tenantId);
    }

    public Intake getIntake(Long id) {
        return sipRepository.findById(id).orElse(null);
    }

    @Transactional
    public Intake updateIntakeStatus(Long intakeId, IntakeStatus status) {
        Intake sip = sipRepository.findById(intakeId)
                .orElseThrow(() -> new IllegalArgumentException("Intake with ID " + intakeId + " does not exist"));
        sip.setStatus(status);
        sip.setUpdatedAt(LocalDateTime.now());
        return sipRepository.save(sip);
    }

    @Transactional
    public boolean deleteIntake(Long id) {
        Intake sip = sipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Intake with ID " + id + " does not exist"));

        // Clear join tables and orphans via native SQL to avoid FK ordering issues
        entityManager.createNativeQuery("DELETE FROM intake_users WHERE intake_id = :intakeId")
                .setParameter("intakeId", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM intake_user_assignments WHERE intake_id = :intakeId")
                .setParameter("intakeId", id).executeUpdate();

        sip.setRootElement(null);
        sipRepository.saveAndFlush(sip);
        sipRepository.delete(sip);
        return true;
    }
}
