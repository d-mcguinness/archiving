package com.dmc.archiving.archive.element;

import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.archive.model.Archive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
                              String title, String description, String createdBy,
                              List<Map<String, Object>> fieldsInput) {
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

        Element savedElement = elementRepository.save(element);

        // Create fields for the element
        if (fieldsInput != null && !fieldsInput.isEmpty()) {
            for (Map<String, Object> fieldInput : fieldsInput) {
                Field field = new Field();
                field.setElement(savedElement);
                field.setName(fieldInput.get("name").toString());
                field.setLabel(fieldInput.get("label") != null ? fieldInput.get("label").toString() : null);
                field.setType(fieldInput.get("type").toString());
                field.setValue(fieldInput.get("value") != null ? fieldInput.get("value").toString() : null);

                savedElement.addField(field);
            }

            // Save again to persist the fields relationship
            savedElement = elementRepository.save(savedElement);
        }

        return savedElement;
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
    public Element updateElement(Long id, String title, String description, String updatedBy, List<Map<String, Object>> fieldsInput) {
        Element element = elementRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Element not found"));

        System.out.println("=== Updating Element ===");
        System.out.println("Element ID: " + id);
        System.out.println("Title: " + title);
        System.out.println("Description: " + description);
        System.out.println("Updated By: " + updatedBy);
        System.out.println("Fields Input: " + fieldsInput);
        System.out.println("Current fields count: " + element.getFields().size());

        element.setTitle(title);
        element.setDescription(description);
        element.setUpdatedBy(updatedBy);
        element.setUpdatedAt(LocalDateTime.now());

        // Update fields if provided
        if (fieldsInput != null && !fieldsInput.isEmpty()) {
            System.out.println("Processing " + fieldsInput.size() + " fields");

            // Clear existing fields
            element.getFields().clear();
            System.out.println("Cleared existing fields");

            // Add updated fields
            for (Map<String, Object> fieldInput : fieldsInput) {
                Field field = new Field();
                field.setElement(element);
                field.setName(fieldInput.get("name").toString());
                field.setLabel(fieldInput.get("label") != null ? fieldInput.get("label").toString() : null);
                field.setType(fieldInput.get("type").toString());
                field.setValue(fieldInput.get("value") != null ? fieldInput.get("value").toString() : null);

                System.out.println("Adding field: " + field.getName() + " = " + field.getValue());
                element.addField(field);
            }

            System.out.println("New fields count: " + element.getFields().size());
        } else {
            System.out.println("No fields to update (fieldsInput is null or empty)");
        }

        Element savedElement = elementRepository.save(element);
        System.out.println("Element saved with " + savedElement.getFields().size() + " fields");
        System.out.println("=== Update Complete ===");

        return savedElement;
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

