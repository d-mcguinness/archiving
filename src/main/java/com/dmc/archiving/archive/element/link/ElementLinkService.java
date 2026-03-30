package com.dmc.archiving.archive.element.link;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.ElementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ElementLinkService {

    private final ElementLinkRepository linkRepository;
    private final ElementRepository elementRepository;

    public ElementLinkService(ElementLinkRepository linkRepository, ElementRepository elementRepository) {
        this.linkRepository = linkRepository;
        this.elementRepository = elementRepository;
    }

    @Transactional
    public ElementLink createLink(Long sourceElementId, Long targetElementId, String linkType,
                                   String label, String description, Boolean directional, String createdBy) {
        Element source = elementRepository.findById(sourceElementId)
                .orElseThrow(() -> new IllegalArgumentException("Source element not found: " + sourceElementId));
        Element target = elementRepository.findById(targetElementId)
                .orElseThrow(() -> new IllegalArgumentException("Target element not found: " + targetElementId));

        if (sourceElementId.equals(targetElementId)) {
            throw new IllegalArgumentException("Cannot link an element to itself");
        }

        // Prevent duplicate links of the same type
        if (linkRepository.existsBySourceElementIdAndTargetElementIdAndLinkType(sourceElementId, targetElementId, linkType)) {
            throw new IllegalArgumentException("Link of type '" + linkType + "' already exists between these elements");
        }

        ElementLink link = new ElementLink();
        link.setSourceElement(source);
        link.setTargetElement(target);
        link.setLinkType(linkType);
        link.setLabel(label);
        link.setDescription(description);
        link.setDirectional(directional != null ? directional : true);
        link.setCreatedBy(createdBy);

        return linkRepository.save(link);
    }

    public List<ElementLink> getLinksByElement(Long elementId) {
        return linkRepository.findBySourceElementIdOrTargetElementId(elementId, elementId);
    }

    public List<ElementLink> getOutgoingLinks(Long elementId) {
        return linkRepository.findBySourceElementId(elementId);
    }

    public List<ElementLink> getIncomingLinks(Long elementId) {
        return linkRepository.findByTargetElementId(elementId);
    }

    public List<ElementLink> getLinksByType(String linkType) {
        return linkRepository.findByLinkType(linkType);
    }

    public ElementLink getLink(Long id) {
        return linkRepository.findById(id).orElse(null);
    }

    @Transactional
    public boolean deleteLink(Long id) {
        if (!linkRepository.existsById(id)) {
            throw new IllegalArgumentException("Link not found: " + id);
        }
        linkRepository.deleteById(id);
        return true;
    }

    @Transactional
    public void deleteAllLinksForElement(Long elementId) {
        linkRepository.deleteBySourceElementIdOrTargetElementId(elementId, elementId);
    }
}
