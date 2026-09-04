package com.dmc.archiving.preservation.api;

import java.util.List;

public interface PreservationExportApi {
    List<PreservationExportFile> exportByTenant(Long tenantId);
    int countByTenant(Long tenantId);
}
