package com.dmc.archiving.scheme.api;

/** The three package lifecycle stages a definition names a root entity for. */
public enum SchemeStage {
    INTAKE, PRESERVATION, RELEASE;

    /** The key used in the JSON {@code stageEntities} object ("intake", …). */
    public String key() {
        return name().toLowerCase();
    }
}
