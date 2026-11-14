package com.dmc.archiving.archive.api;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.ElementRepository;
import com.dmc.archiving.archive.element.ElementService;
import com.dmc.archiving.archive.element.link.ElementLinkRepository;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.repository.ArchiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
class ArchiveElementDepositApiImpl implements ArchiveElementDepositApi {

    private final ArchiveRepository archiveRepository;
    private final ElementRepository elementRepository;
    private final ElementService elementService;
    private final ElementLinkRepository linkRepository;

    ArchiveElementDepositApiImpl(ArchiveRepository archiveRepository, ElementRepository elementRepository,
                                 ElementService elementService, ElementLinkRepository linkRepository) {
        this.archiveRepository = archiveRepository;
        this.elementRepository = elementRepository;
        this.elementService = elementService;
        this.linkRepository = linkRepository;
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

    @Override
    @Transactional
    public int removeDeposited(Long tenantId, String metadata) {
        if (tenantId == null || metadata == null || metadata.isBlank()) {
            return 0;
        }
        List<Element> deposited = elementRepository.findByTenantAndMetadata(tenantId, metadata);
        Set<Long> ids = new HashSet<>();
        for (Element element : deposited) {
            ids.add(element.getId());
        }
        for (Element element : deposited) {
            // One nested under another of the same deposit goes with its parent.
            if (hasAncestorIn(element, ids)) {
                continue;
            }
            removeLinks(element);
            elementService.deleteElement(element.getId());
        }
        return deposited.size();
    }

    private static boolean hasAncestorIn(Element element, Set<Long> ids) {
        for (Element p = element.getParent(); p != null; p = p.getParent()) {
            if (ids.contains(p.getId())) {
                return true;
            }
        }
        return false;
    }

    /** Links are not cascaded from their target, so clear them for the whole subtree first. */
    private void removeLinks(Element element) {
        linkRepository.deleteBySourceElementIdOrTargetElementId(element.getId(), element.getId());
        for (Element child : element.getChildren()) {
            removeLinks(child);
        }
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
