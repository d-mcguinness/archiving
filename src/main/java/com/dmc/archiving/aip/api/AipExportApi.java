package com.dmc.archiving.aip.api;

import java.util.List;

public interface AipExportApi {
    List<AipExportFile> exportByTenant(Long tenantId);
    int countByTenant(Long tenantId);
}
