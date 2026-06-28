package com.dmc.archiving.release.api;

import java.util.List;

public interface ReleaseExportApi {
    List<ReleaseExportFile> exportByTenant(Long tenantId);
    int countByTenant(Long tenantId);
}
