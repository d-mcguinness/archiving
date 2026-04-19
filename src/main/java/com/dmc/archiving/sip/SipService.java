package com.dmc.archiving.sip;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.sip.input.CreateSipInput;
import com.dmc.archiving.user.model.User;
import com.dmc.archiving.sip.model.Sip;
import com.dmc.archiving.sip.model.SipStatus;
import com.dmc.archiving.sip.generator.SipGenerator;
import com.dmc.archiving.sip.generator.SipGeneratorFactory;
import com.dmc.archiving.sip.repository.SipRepository;
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
public class SipService {

    private final SipRepository sipRepository;
    private final UserApi userApi;
    private final SipGeneratorFactory sipGeneratorFactory;
    private final EntityManager entityManager;

    public SipService(SipRepository sipRepository, UserApi userApi, SipGeneratorFactory sipGeneratorFactory, EntityManager entityManager) {
        this.sipRepository = sipRepository;
        this.userApi = userApi;
        this.sipGeneratorFactory = sipGeneratorFactory;
        this.entityManager = entityManager;
    }

    @Transactional
    public String generateSip(Long sipId) {
        Sip sip = sipRepository.findById(sipId)
                .orElseThrow(() -> new IllegalArgumentException("Sip not found: " + sipId));
        SipGenerator generator = sipGeneratorFactory.getGenerator(sip.getStandard());
        return generator.generate(sip);
    }

    @Transactional
    public Sip createSip(CreateSipInput input) {
        User user = userApi.getUserById(input.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + input.getUserId() + " does not exist"));

        Long ownerId = input.getOwnerId() != null ? input.getOwnerId() : input.getUserId();
        Long tenantId = input.getTenantId() != null ? input.getTenantId() : ownerId;

        LocalDateTime now = LocalDateTime.now();

        Sip sip = new Sip();
        sip.setTenantId(tenantId);
        sip.setOwnerId(ownerId);
        sip.setArchiveId(input.getArchiveId());
        sip.setTitle(input.getTitle());
        sip.setDescription(input.getDescription());
        sip.setContent(input.getContent());
        sip.setCreatedAt(now);
        sip.setUpdatedAt(now);
        sip.setStatus(SipStatus.DRAFT);
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

    private static @NonNull Element getRootElement(CreateSipInput input, LocalDateTime now) {
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

    public List<Sip> getAllSips() {
        return sipRepository.findAll();
    }

    public List<Sip> getSipsByTenant(Long tenantId) {
        return sipRepository.findByTenantId(tenantId);
    }

    public Sip getSip(Long id) {
        return sipRepository.findById(id).orElse(null);
    }

    @Transactional
    public Sip updateSipStatus(Long sipId, SipStatus status) {
        Sip sip = sipRepository.findById(sipId)
                .orElseThrow(() -> new IllegalArgumentException("Sip with ID " + sipId + " does not exist"));
        sip.setStatus(status);
        sip.setUpdatedAt(LocalDateTime.now());
        return sipRepository.save(sip);
    }

    @Transactional
    public boolean deleteSip(Long id) {
        Sip sip = sipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sip with ID " + id + " does not exist"));

        // Clear join tables and orphans via native SQL to avoid FK ordering issues
        entityManager.createNativeQuery("DELETE FROM sip_users WHERE sip_id = :sipId")
                .setParameter("sipId", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM sip_user_assignments WHERE sip_id = :sipId")
                .setParameter("sipId", id).executeUpdate();

        sip.setRootElement(null);
        sipRepository.saveAndFlush(sip);
        sipRepository.delete(sip);
        return true;
    }
}
