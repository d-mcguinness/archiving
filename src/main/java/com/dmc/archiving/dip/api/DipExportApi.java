package com.dmc.archiving.dip.api;

import java.util.List;

public interface DipExportApi {
    List<DipExportFile> exportByTenant(Long tenantId);
    int countByTenant(Long tenantId);
}
