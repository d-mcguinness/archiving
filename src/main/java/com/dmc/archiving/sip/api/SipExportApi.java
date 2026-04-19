package com.dmc.archiving.sip.api;

import java.util.List;

public interface SipExportApi {
    List<SipExportFile> exportByTenant(Long tenantId);
    int countByTenant(Long tenantId);
}
