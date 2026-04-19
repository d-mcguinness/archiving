package com.dmc.archiving.dip.api;

import com.dmc.archiving.archive.element.ElementSerializer;
import com.dmc.archiving.dip.DipService;
import com.dmc.archiving.dip.model.Dip;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class DipExportApiImpl implements DipExportApi {

    private final DipService dipService;

    DipExportApiImpl(DipService dipService) {
        this.dipService = dipService;
    }

    @Override
    public List<DipExportFile> exportByTenant(Long tenantId) {
        return dipService.getDipsByTenant(tenantId).stream()
                .map(this::toFile)
                .toList();
    }

    @Override
    public int countByTenant(Long tenantId) {
        return dipService.getDipsByTenant(tenantId).size();
    }

    private DipExportFile toFile(Dip dip) {
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
        if (dip.getSourceAipId() != null) data.put("sourceAipId", dip.getSourceAipId());
        if (dip.getRootElement() != null) {
            data.put("rootElement", ElementSerializer.serialize(dip.getRootElement()));
        }
        return new DipExportFile(dip.getId(), dip.getTitle(), data);
    }
}
