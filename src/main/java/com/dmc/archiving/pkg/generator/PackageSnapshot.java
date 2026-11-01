package com.dmc.archiving.pkg.generator;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flat, immutable, stage-agnostic view of an archival package (Intake,
 * Preservation, Release, or the unified ArchivalPackage) for generators.
 *
 * Generators depend on this instead of on JPA entities, so one generator
 * implementation can serve every entity type that can produce a snapshot.
 * The root element is carried both flattened (name → value map, for JSON
 * emitters) and as {@link FieldSnapshot}s (name/label/type/value), so a
 * generator that needs an {@link Element} graph — the Noark 5 XML writer
 * does — can rebuild one with {@link #toElement()} without losing fields.
 */
public record PackageSnapshot(
        // Envelope
        Long id,
        String title,
        String description,
        String status,
        String standard,
        String createdAt,
        String updatedAt,

        /** Upstream package this one was derived from (Intake → Preservation → Release); null for an Intake. */
        Long sourceId,

        // Root element
        String elementIdentifier,
        String entityName,
        String entityType,
        String norwegianName,
        String englishName,
        String elementTitle,
        String elementDescription,

        /** Root-element fields, flattened to name → value in declaration order. */
        Map<String, String> fields,

        /** Root-element fields with their label and type preserved. */
        List<FieldSnapshot> fieldList,

        // Provenance (from root element)
        String elementCreatedAt,
        String elementCreatedBy,
        String elementStatus
) {

    public record FieldSnapshot(String name, String label, String type, String value) {
        static FieldSnapshot from(Field f) {
            return new FieldSnapshot(f.getName(), f.getLabel(), f.getType(), f.getValue());
        }
    }

    public PackageSnapshot {
        fields = fields == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        fieldList = fieldList == null ? Collections.emptyList() : List.copyOf(fieldList);
    }

    /**
     * Build a snapshot from the envelope values every package entity carries
     * plus its (possibly null) root element. Entity adapters call this; the
     * record constructor is for tests and hand-built snapshots.
     */
    public static PackageSnapshot of(Long id,
                                     String title,
                                     String description,
                                     Enum<?> status,
                                     Enum<?> standard,
                                     String createdAt,
                                     String updatedAt,
                                     Long sourceId,
                                     Element root) {
        Map<String, String> fields = Collections.emptyMap();
        List<FieldSnapshot> fieldList = Collections.emptyList();
        String elementIdentifier = null, entityName = null, entityType = null,
                norwegianName = null, englishName = null, elementTitle = null, elementDescription = null,
                elementCreatedAt = null, elementCreatedBy = null, elementStatus = null;

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
                List<FieldSnapshot> list = new ArrayList<>(rootFields.size());
                for (Field field : rootFields) {
                    fieldMap.put(field.getName(), field.getValue());
                    list.add(FieldSnapshot.from(field));
                }
                fields = fieldMap;
                fieldList = list;
            }
        }

        return new PackageSnapshot(
                id, title, description,
                status != null ? status.name() : null,
                standard != null ? standard.name() : "UNKNOWN",
                createdAt, updatedAt, sourceId,
                elementIdentifier, entityName, entityType, norwegianName, englishName,
                elementTitle, elementDescription,
                fields, fieldList,
                elementCreatedAt, elementCreatedBy, elementStatus
        );
    }

    public boolean hasRootElement() {
        return entityName != null;
    }

    public boolean hasFields() {
        return !fields.isEmpty();
    }

    /**
     * Rebuild a detached {@link Element} (with its {@link Field}s) from this
     * snapshot, for emitters that walk the entity graph. Returns null when the
     * package has no root element. The result is not persisted or managed.
     */
    public Element toElement() {
        if (!hasRootElement()) return null;
        Element e = new Element();
        e.setElementIdentifier(elementIdentifier);
        e.setEntityName(entityName);
        e.setEntityType(entityType);
        e.setNorwegianName(norwegianName);
        e.setEnglishName(englishName);
        e.setTitle(elementTitle);
        e.setDescription(elementDescription);
        e.setCreatedBy(elementCreatedBy);
        e.setStatus(elementStatus);
        e.setIsRoot(true);
        for (FieldSnapshot fs : fieldList) {
            Field f = new Field();
            f.setName(fs.name());
            f.setLabel(fs.label());
            f.setType(fs.type());
            f.setValue(fs.value());
            e.addField(f);
        }
        return e;
    }
}
