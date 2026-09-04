package com.dmc.archiving.intake.api;

import java.util.List;

public interface IntakeExportApi {
    List<IntakeExportFile> exportByTenant(Long tenantId);
    int countByTenant(Long tenantId);
}
