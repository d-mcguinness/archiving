package com.dmc.archiving.preservation.api;

import java.util.List;
import java.util.Optional;

public interface PreservationExportApi {
    List<PreservationExportFile> exportByTenant(Long tenantId);
    int countByTenant(Long tenantId);

    /** One preservation with its standard-conformant artifact rendered; empty when it does not exist. */
    Optional<PreservationExtract> extract(Long preservationId);
}
