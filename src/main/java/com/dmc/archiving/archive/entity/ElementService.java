package com.dmc.archiving.archive.entity;

import com.dmc.archiving.archive.model.Archive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ElementService {

    private final ElementRepository elementRepository;

    /**
     * Create a new element in an archive
     */
    @Transactional
    public Element createElement(Archive archive, Element parent,
                              String elementIdentifier, String entityName, String entityType,
                              String norwegianName, String englishName,
                              String title, String description, String createdBy) {
        Element element = new Element();
        element.setArchive(archive);
        element.setParent(parent);
        element.setElementIdentifier(elementIdentifier);
        element.setEntityName(entityName);
        element.setEntityType(entityType);
        element.setNorwegianName(norwegianName);
        element.setEnglishName(englishName);
        element.setTitle(title);
        element.setDescription(description);
        element.setCreatedBy(createdBy);
        element.setCreatedAt(LocalDateTime.now());
        element.setIsRoot(parent == null);
        element.setStatus("Opprettet");

        if (parent != null) {
            parent.addChild(element);
        }

        return elementRepository.save(element);
    }

    /**
     * Get element by ID
     */
    public Optional<Element> getElement(Long id) {
        return elementRepository.findById(id);
    }

    /**
     * Get all elements for an archive
     */
    public List<Element> getElementsByArchive(Long archiveId) {
        return elementRepository.findByArchiveId(archiveId);
    }

    /**
     * Get root elements for an archive
     */
    public List<Element> getRootElements(Long archiveId) {
        return elementRepository.findByArchiveIdAndIsRootTrue(archiveId);
    }

    /**
     * Get children of an element
     */
    public List<Element> getChildren(Long elementId) {
        return elementRepository.findByParentId(elementId);
    }

    /**
     * Get element by archive and identifier
     */
    public Optional<Element> getElementByIdentifier(Long archiveId, String elementIdentifier) {
        return elementRepository.findByArchiveIdAndElementIdentifier(archiveId, elementIdentifier);
    }

    /**
     * Update element
     */
    @Transactional
    public Element updateElement(Long id, String title, String description, String updatedBy) {
        Element element = elementRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Element not found"));

        element.setTitle(title);
        element.setDescription(description);
        element.setUpdatedBy(updatedBy);
        element.setUpdatedAt(LocalDateTime.now());

        return elementRepository.save(element);
    }

    /**
     * Update element status
     */
    @Transactional
    public Element updateStatus(Long id, String status, String updatedBy) {
        Element element = elementRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Element not found"));

        element.setStatus(status);
        element.setUpdatedBy(updatedBy);
        element.setUpdatedAt(LocalDateTime.now());

        return elementRepository.save(element);
    }

    /**
     * Close an element
     */
    @Transactional
    public Element closeElement(Long id, String closedBy) {
        Element element = elementRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Element not found"));

        element.setStatus("Avsluttet");
        element.setClosedAt(LocalDateTime.now());
        element.setClosedBy(closedBy);

        return elementRepository.save(element);
    }

    /**
     * Delete an element and all its children
     */
    @Transactional
    public void deleteElement(Long id) {
        Element element = elementRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Element not found"));

        if (element.getParent() != null) {
            element.getParent().removeChild(element);
        }

        elementRepository.delete(element);
    }

    /**
     * Search elements by title or description
     */
    public List<Element> searchElements(Long archiveId, String searchTerm) {
        return elementRepository.searchByTitleOrDescription(archiveId, searchTerm);
    }

    /**
     * Get elements by status
     */
    public List<Element> getElementsByStatus(Long archiveId, String status) {
        return elementRepository.findByArchiveIdAndStatus(archiveId, status);
    }

    /**
     * Get full hierarchy for an archive
     */
    public List<Element> getHierarchy(Long archiveId) {
        return elementRepository.findHierarchyByArchive(archiveId);
    }

    /**
     * Count elements in an archive
     */
    public long countElements(Long archiveId) {
        return elementRepository.countByArchiveId(archiveId);
    }

    /**
     * Get leaf elements (without children)
     */
    public List<Element> getLeafElements(Long archiveId) {
        return elementRepository.findLeafElementsByArchive(archiveId);
    }

    /**
     * Move element to a new parent
     */
    @Transactional
    public Element moveElement(Long elementId, Long newParentId, String updatedBy) {
        Element element = elementRepository.findById(elementId)
            .orElseThrow(() -> new IllegalArgumentException("Element not found"));

        Element newParent = newParentId != null ?
            elementRepository.findById(newParentId)
                .orElseThrow(() -> new IllegalArgumentException("New parent not found")) : null;

        // Validate move
        if (newParent != null) {
            if (!newParent.getArchive().getId().equals(element.getArchive().getId())) {
                throw new IllegalArgumentException("Cannot move element to different archive");
            }
        }

        // Remove from old parent
        if (element.getParent() != null) {
            element.getParent().removeChild(element);
        }

        // Set new parent
        element.setParent(newParent);
        if (newParent != null) {
            newParent.addChild(element);
        }

        element.setUpdatedBy(updatedBy);
        element.setUpdatedAt(LocalDateTime.now());

        return elementRepository.save(element);
    }
}

