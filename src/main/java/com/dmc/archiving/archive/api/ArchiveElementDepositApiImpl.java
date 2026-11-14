package com.dmc.archiving.archive.api;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.ElementRepository;
import com.dmc.archiving.archive.element.ElementService;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.repository.ArchiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
class ArchiveElementDepositApiImpl implements ArchiveElementDepositApi {

    private final ArchiveRepository archiveRepository;
    private final ElementRepository elementRepository;
    private final ElementService elementService;

    ArchiveElementDepositApiImpl(ArchiveRepository archiveRepository, ElementRepository elementRepository,
                                 ElementService elementService) {
        this.archiveRepository = archiveRepository;
        this.elementRepository = elementRepository;
        this.elementService = elementService;
    }

    @Override
    @Transactional
    public List<Long> deposit(Long archiveId, List<ElementDeposit> elements, String createdBy) {
        Archive archive = archiveRepository.findById(archiveId)
                .orElseThrow(() -> new IllegalArgumentException("Archive not found with ID: " + archiveId));

        Map<String, Element> addedByEntity = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        for (ElementDeposit deposit : elements) {
            Element parent = parentFor(archive, deposit.parentEntityName(), addedByEntity);
            List<Map<String, Object>> fields = new ArrayList<>();
            for (ElementDeposit.Field f : deposit.fields()) {
                Map<String, Object> field = new HashMap<>();
                field.put("name", f.name());
                field.put("label", f.label());
                field.put("type", f.type());
                field.put("value", f.value());
                fields.add(field);
            }
            Element element = elementService.createElement(archive, parent,
                    deposit.elementIdentifier(), deposit.entityName(), deposit.entityType(),
                    null, deposit.entityName(), deposit.title(), null, createdBy, fields);
            if (deposit.metadata() != null) {
                element.setMetadata(deposit.metadata());
                element = elementRepository.save(element);
            }
            addedByEntity.put(deposit.entityName(), element);
            ids.add(element.getId());
        }
        return ids;
    }

    private Element parentFor(Archive archive, String parentEntityName, Map<String, Element> addedByEntity) {
        if (parentEntityName != null) {
            Element added = addedByEntity.get(parentEntityName);
            if (added != null) {
                return added;
            }
            Element existing = elementRepository
                    .findFirstByArchiveIdAndEntityNameOrderByCreatedAtDescIdDesc(archive.getId(), parentEntityName)
                    .orElse(null);
            if (existing != null) {
                return existing;
            }
        }
        return archive.getRootElement();
    }
}
