package com.dmc.archiving.aip.api;

import com.dmc.archiving.aip.AipService;
import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.archive.element.ElementSerializer;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class AipExportApiImpl implements AipExportApi {

    private final AipService aipService;

    AipExportApiImpl(AipService aipService) {
        this.aipService = aipService;
    }

    @Override
    public List<AipExportFile> exportByTenant(Long tenantId) {
        return aipService.getAipsByTenant(tenantId).stream()
                .map(this::toFile)
                .toList();
    }

    @Override
    public int countByTenant(Long tenantId) {
        return aipService.getAipsByTenant(tenantId).size();
    }

    private AipExportFile toFile(Aip aip) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", aip.getId());
        data.put("stage", "AIP");
        data.put("title", aip.getTitle());
        data.put("description", aip.getDescription());
        data.put("standard", aip.getStandard().name());
        data.put("status", aip.getStatus().name());
        data.put("ownerId", aip.getOwnerId());
        data.put("tenantId", aip.getTenantId());
        data.put("createdAt", aip.getCreatedAt());
        data.put("updatedAt", aip.getUpdatedAt());
        if (aip.getSourceSipId() != null) data.put("sourceSipId", aip.getSourceSipId());
        if (aip.getRootElement() != null) {
            data.put("rootElement", ElementSerializer.serialize(aip.getRootElement()));
        }
        return new AipExportFile(aip.getId(), aip.getTitle(), data);
    }
}
