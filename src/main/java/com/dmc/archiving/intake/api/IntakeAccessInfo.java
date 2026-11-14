package com.dmc.archiving.intake.api;

/**
 * What another module needs to decide whether a user may put content into an
 * intake: the tenant that owns it, and whether the user owns or is assigned to it.
 */
public record IntakeAccessInfo(Long intakeId, Long tenantId, boolean ownerOrAssigned) {
}
