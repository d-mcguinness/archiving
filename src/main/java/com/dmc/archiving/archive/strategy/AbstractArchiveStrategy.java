package com.dmc.archiving.archive.strategy;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.archive.model.Archive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base implementation of ArchiveStrategy with common functionality
 */
public abstract class AbstractArchiveStrategy implements ArchiveStrategy {

    @Override
    public ValidationResult validate(Archive archive) {
        ValidationResult result = new ValidationResult();

        // Common validation
        if (archive.getTitle() == null || archive.getTitle().trim().isEmpty()) {
            result.addError("Archive title is required");
        }

        if (archive.getOwnerId() == null) {
            result.addError("Archive owner is required");
        }

        // Standard-specific validation
        validateStandard(archive, result);

        return result;
    }

    @Override
    public Map<String, Object> export(Archive archive) {
        Map<String, Object> exportData = new LinkedHashMap<>();

        // Common export fields
        exportData.put("id", archive.getId());
        exportData.put("title", archive.getTitle());
        exportData.put("description", archive.getDescription());
        exportData.put("standard", archive.getStandard());
        exportData.put("status", archive.getStatus());
        exportData.put("ownerId", archive.getOwnerId());
        exportData.put("tenantId", archive.getTenantId());
        exportData.put("createdAt", archive.getCreatedAt());
        exportData.put("updatedAt", archive.getUpdatedAt());

        // Export element tree
        if (archive.getRootElement() != null) {
            exportData.put("rootElement", exportElement(archive.getRootElement()));
        } else if (archive.getElements() != null && !archive.getElements().isEmpty()) {
            // Fallback: export flat elements if no root element is set
            List<Map<String, Object>> elementsList = new ArrayList<>();
            for (Element element : archive.getElements()) {
                if (element.getParent() == null) {
                    // Top-level elements (no parent)
                    elementsList.add(exportElement(element));
                }
            }
            if (!elementsList.isEmpty()) {
                exportData.put("elements", elementsList);
            }
        }

        // Include raw content if present (for archives without element tree)
        if (archive.getContent() != null && !archive.getContent().isEmpty()) {
            exportData.put("content", archive.getContent());
        }

        // Standard-specific export
        exportStandard(archive, exportData);

        return exportData;
    }

    /**
     * Recursively export an element and its children/fields
     */
    protected Map<String, Object> exportElement(Element element) {
        Map<String, Object> elementData = new LinkedHashMap<>();
        elementData.put("id", element.getId());
        elementData.put("elementIdentifier", element.getElementIdentifier());
        elementData.put("entityName", element.getEntityName());
        elementData.put("entityType", element.getEntityType());
        elementData.put("title", element.getTitle());

        if (element.getDescription() != null) {
            elementData.put("description", element.getDescription());
        }
        if (element.getNorwegianName() != null) {
            elementData.put("norwegianName", element.getNorwegianName());
        }
        if (element.getCreatedAt() != null) {
            elementData.put("createdAt", element.getCreatedAt());
        }
        if (element.getCreatedBy() != null) {
            elementData.put("createdBy", element.getCreatedBy());
        }

        // Export fields
        if (element.getFields() != null && !element.getFields().isEmpty()) {
            Map<String, Object> fields = new LinkedHashMap<>();
            for (Field field : element.getFields()) {
                if (field.getValue() != null && !field.getValue().isEmpty()) {
                    fields.put(field.getName(), Map.of(
                        "label", field.getLabel() != null ? field.getLabel() : field.getName(),
                        "type", field.getType() != null ? field.getType() : "text",
                        "value", field.getValue()
                    ));
                }
            }
            if (!fields.isEmpty()) {
                elementData.put("fields", fields);
            }
        }

        // Recursively export children
        if (element.getChildren() != null && !element.getChildren().isEmpty()) {
            List<Map<String, Object>> children = new ArrayList<>();
            for (Element child : element.getChildren()) {
                children.add(exportElement(child));
            }
            elementData.put("children", children);
        }

        return elementData;
    }

    @Override
    public Map<String, Object> transformToStandard(Archive archive) {
        Map<String, Object> transformed = new HashMap<>();

        // Base transformation
        transformed.put("standardName", getStandardName());
        transformed.put("archiveId", archive.getId());
        transformed.put("title", archive.getTitle());

        // Standard-specific transformation
        addStandardSpecificFields(archive, transformed);

        return transformed;
    }

    /**
     * Perform standard-specific validation
     */
    protected abstract void validateStandard(Archive archive, ValidationResult result);

    /**
     * Add standard-specific fields to export
     */
    protected abstract void exportStandard(Archive archive, Map<String, Object> exportData);

    /**
     * Add standard-specific transformation fields
     */
    protected abstract void addStandardSpecificFields(Archive archive, Map<String, Object> transformed);
}
