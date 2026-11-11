package com.dmc.archiving.scheme.api;

import java.util.List;

/**
 * One typed field of an entity. {@code type} is "string", "text", "date" or "number".
 *
 * A field whose values come from one of the standard's controlled vocabularies
 * names it in {@code vocabulary} (a dot path into the definition, e.g.
 * {@code statusValues.archiveStatus}); the loader resolves it to {@code options},
 * which the UI renders as a select instead of free text. Both are empty for
 * free-text fields.
 */
public record FieldDefinition(String name, String label, String type, boolean required,
                              String vocabulary, List<FieldOption> options) {

    public FieldDefinition {
        options = options == null ? List.of() : List.copyOf(options);
    }

    /** Convenience for free-text fields. */
    public FieldDefinition(String name, String label, String type, boolean required) {
        this(name, label, type, required, null, List.of());
    }

    public boolean isEnumerated() {
        return !options.isEmpty();
    }
}
