package com.dmc.archiving.scheme.api;

import com.dmc.archiving.archive.model.ArchiveStandard;

import java.util.List;
import java.util.Optional;

/** Read-only access to the archive standard definitions bundled with the backend. */
public interface SchemeDefinitionApi {

    /** All definitions, in {@link ArchiveStandard} declaration order. */
    List<SchemeDefinition> all();

    Optional<SchemeDefinition> get(ArchiveStandard standard);

    /** Shortcut: the root entity for {@code stage} of {@code standard}. */
    default Optional<EntityDefinition> stageEntity(ArchiveStandard standard, SchemeStage stage) {
        return get(standard).flatMap(d -> d.stageEntity(stage));
    }
}
