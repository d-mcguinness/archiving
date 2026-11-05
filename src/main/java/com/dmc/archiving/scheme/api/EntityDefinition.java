package com.dmc.archiving.scheme.api;

import java.util.List;

/**
 * One entity of a standard (e.g. Noark 5 "Archive"): its structural role,
 * the entities allowed beneath it, and its fields.
 */
public record EntityDefinition(
        String name,
        String type,
        String description,
        String parent,
        List<String> children,
        String note,
        List<FieldDefinition> fields
) {
    public EntityDefinition {
        children = children == null ? List.of() : List.copyOf(children);
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
