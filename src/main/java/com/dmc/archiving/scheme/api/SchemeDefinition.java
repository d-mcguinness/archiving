package com.dmc.archiving.scheme.api;

import com.dmc.archiving.archive.model.ArchiveStandard;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A complete standard definition as loaded from {@code schemes/<file>.json}.
 *
 * @param standard      the enum constant this definition describes
 * @param displayName   the short display name used in the UI ("Dublin Core", "ISAD(G)")
 * @param stageEntities root entity name per lifecycle stage ("intake" → "Archive", …)
 * @param vocabularies  the standard-specific controlled vocabularies, verbatim from the
 *                      file (heterogeneous shapes, so kept as JSON-like maps/lists)
 */
public record SchemeDefinition(
        ArchiveStandard standard,
        String displayName,
        String fullName,
        String reference,
        String description,
        String version,
        String xsdReference,
        String url,
        Map<String, String> stageEntities,
        List<EntityDefinition> entities,
        Map<String, Object> vocabularies
) {
    public SchemeDefinition {
        stageEntities = stageEntities == null ? Map.of() : Map.copyOf(stageEntities);
        entities = entities == null ? List.of() : List.copyOf(entities);
        vocabularies = vocabularies == null ? Map.of() : Map.copyOf(vocabularies);
    }

    public Optional<EntityDefinition> entity(String name) {
        return entities.stream().filter(e -> e.name().equals(name)).findFirst();
    }

    /** The root entity a package of {@code stage} is built from, if the definition names one. */
    public Optional<EntityDefinition> stageEntity(SchemeStage stage) {
        String name = stageEntities.get(stage.key());
        return name == null ? Optional.empty() : entity(name);
    }

    /** Entities allowed as children of {@code parentName}, in the order the parent lists them. */
    public List<EntityDefinition> childEntities(String parentName) {
        return entity(parentName)
                .map(p -> p.children().stream().flatMap(n -> entity(n).stream()).toList())
                .orElse(List.of());
    }
}
