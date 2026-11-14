package com.dmc.archiving.archive.api;

import java.util.List;

/**
 * One element another module asks the archive to add to its tree.
 *
 * @param entityName       the standard's entity this element is an instance of ("Registration")
 * @param entityType       that entity's structural type ("record_unit")
 * @param parentEntityName the entity the standard nests this one under, or null for a top-level entity
 * @param metadata         opaque JSON kept on the element (e.g. which document it describes), or null
 */
public record ElementDeposit(
        String entityName,
        String entityType,
        String parentEntityName,
        String elementIdentifier,
        String title,
        String metadata,
        List<Field> fields
) {
    public ElementDeposit {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    /** One answered field of the element. */
    public record Field(String name, String label, String type, String value) {
    }
}
