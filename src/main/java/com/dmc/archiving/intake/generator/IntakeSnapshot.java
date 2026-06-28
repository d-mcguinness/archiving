package com.dmc.archiving.intake.generator;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.intake.model.Intake;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flat, immutable snapshot of a SIP for use by generators.
 * Decouples generators from JPA entities and eliminates duplicated extraction logic.
 */
public record IntakeSnapshot(
        // Envelope
        Long id,
        String title,
        String description,
        String status,
        String standard,
        String createdAt,
        String updatedAt,

        // Root element
        String elementIdentifier,
        String entityName,
        String entityType,
        String norwegianName,
        String englishName,
        String elementTitle,
        String elementDescription,

        // Fields (flattened from root element)
        Map<String, String> fields,

        // Provenance (from root element)
        String elementCreatedAt,
        String elementCreatedBy,
        String elementStatus
) {
    public static IntakeSnapshot from(Intake sip) {
        Element root = sip.getRootElement();

        Map<String, String> fields = Collections.emptyMap();
        String elementIdentifier = null;
        String entityName = null;
        String entityType = null;
        String norwegianName = null;
        String englishName = null;
        String elementTitle = null;
        String elementDescription = null;
        String elementCreatedAt = null;
        String elementCreatedBy = null;
        String elementStatus = null;

        if (root != null) {
            elementIdentifier = root.getElementIdentifier();
            entityName = root.getEntityName();
            entityType = root.getEntityType();
            norwegianName = root.getNorwegianName();
            englishName = root.getEnglishName();
            elementTitle = root.getTitle();
            elementDescription = root.getDescription();
            elementCreatedAt = root.getCreatedAt() != null ? root.getCreatedAt().toString() : null;
            elementCreatedBy = root.getCreatedBy();
            elementStatus = root.getStatus();

            List<Field> rootFields = root.getFields();
            if (rootFields != null && !rootFields.isEmpty()) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                for (Field field : rootFields) {
                    fieldMap.put(field.getName(), field.getValue());
                }
                fields = Collections.unmodifiableMap(fieldMap);
            }
        }

        return new IntakeSnapshot(
                sip.getId(),
                sip.getTitle(),
                sip.getDescription(),
                sip.getStatus().name(),
                sip.getStandard() != null ? sip.getStandard().name() : "UNKNOWN",
                sip.getCreatedAtString(),
                sip.getUpdatedAtString(),
                elementIdentifier, entityName, entityType, norwegianName, englishName,
                elementTitle, elementDescription,
                fields,
                elementCreatedAt, elementCreatedBy, elementStatus
        );
    }

    public boolean hasRootElement() {
        return entityName != null;
    }

    public boolean hasFields() {
        return !fields.isEmpty();
    }
}
