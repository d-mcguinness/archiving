package com.dmc.archiving.intake.api;

/**
 * Read-only intake access facts for other modules (e.g. linking a document to
 * an intake). The caller applies its own role rules on top.
 */
public interface IntakeAccessApi {

    /** Returns null if the intake does not exist. */
    IntakeAccessInfo accessFor(Long intakeId, Long userId);
}
