package com.dmc.archiving.sip;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.archive.model.UserRole;
import com.dmc.archiving.sip.input.CreateSipInput;
import com.dmc.archiving.sip.model.Sip;
import com.dmc.archiving.sip.model.SipStatus;
import com.dmc.archiving.sip.repository.SipRepository;
import com.dmc.archiving.user.api.UserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SipService {

    private final SipRepository sipRepository;
    private final UserApi userApi;

    public SipService(SipRepository sipRepository, UserApi userApi) {
        this.sipRepository = sipRepository;
        this.userApi = userApi;
    }

    @Transactional
    public Sip createSip(CreateSipInput input) {
        if (!userApi.userExists(input.getUserId())) {
            throw new IllegalArgumentException("User with ID " + input.getUserId() + " does not exist");
        }

        Long ownerId = input.getOwnerId() != null ? input.getOwnerId() : input.getUserId();
        Long tenantId = input.getTenantId() != null ? input.getTenantId() : ownerId;

        LocalDateTime now = LocalDateTime.now();

        Sip sip = new Sip();
        sip.setTenantId(tenantId);
        sip.setOwnerId(ownerId);
        sip.setTitle(input.getTitle());
        sip.setDescription(input.getDescription());
        sip.setContent(input.getContent());
        sip.setCreatedAt(now);
        sip.setUpdatedAt(now);
        sip.setStatus(SipStatus.DRAFT);
        sip.setStandard(input.getStandard());

        // Assign creator as OWNER
        sip.assignUser(input.getUserId(), UserRole.OWNER);

        // Create root element inline
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
        if (!sipRepository.existsById(id)) {
            throw new IllegalArgumentException("Sip with ID " + id + " does not exist");
        }
        sipRepository.deleteById(id);
        return true;
    }
}
