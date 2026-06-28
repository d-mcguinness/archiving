package com.dmc.archiving.release.api;

import com.dmc.archiving.archive.element.ElementSerializer;
import com.dmc.archiving.release.ReleaseService;
import com.dmc.archiving.release.model.Release;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class ReleaseExportApiImpl implements ReleaseExportApi {

    private final ReleaseService dipService;

    ReleaseExportApiImpl(ReleaseService dipService) {
        this.dipService = dipService;
    }

    @Override
    public List<ReleaseExportFile> exportByTenant(Long tenantId) {
        return dipService.getReleasesByTenant(tenantId).stream()
                .map(this::toFile)
                .toList();
    }

    @Override
    public int countByTenant(Long tenantId) {
        return dipService.getReleasesByTenant(tenantId).size();
    }

    private ReleaseExportFile toFile(Release dip) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", dip.getId());
        data.put("stage", "DIP");
        data.put("title", dip.getTitle());
        data.put("description", dip.getDescription());
        data.put("standard", dip.getStandard().name());
        data.put("status", dip.getStatus().name());
        data.put("ownerId", dip.getOwnerId());
        data.put("tenantId", dip.getTenantId());
        data.put("createdAt", dip.getCreatedAt());
        data.put("updatedAt", dip.getUpdatedAt());
        if (dip.getSourcePreservationId() != null) data.put("sourcePreservationId", dip.getSourcePreservationId());
        if (dip.getRootElement() != null) {
            data.put("rootElement", ElementSerializer.serialize(dip.getRootElement()));
        }
        return new ReleaseExportFile(dip.getId(), dip.getTitle(), data);
    }
}
