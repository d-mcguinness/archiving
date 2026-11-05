package com.dmc.archiving.intake.api;

import java.util.List;
import java.util.Optional;

public interface IntakeExportApi {
    List<IntakeExportFile> exportByTenant(Long tenantId);
    int countByTenant(Long tenantId);

    /** The archive an intake was created from, when the intake exists and is linked to one. */
    Optional<Long> archiveIdOf(Long intakeId);
}
