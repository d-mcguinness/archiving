package com.dmc.archiving.preservation.api;

import com.dmc.archiving.preservation.PreservationService;
import com.dmc.archiving.preservation.model.Preservation;
import com.dmc.archiving.archive.element.ElementSerializer;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class PreservationExportApiImpl implements PreservationExportApi {

    private final PreservationService aipService;

    PreservationExportApiImpl(PreservationService aipService) {
        this.aipService = aipService;
    }

    @Override
    public List<PreservationExportFile> exportByTenant(Long tenantId) {
        return aipService.getPreservationsByTenant(tenantId).stream()
                .map(this::toFile)
                .toList();
    }

    @Override
    public int countByTenant(Long tenantId) {
        return aipService.getPreservationsByTenant(tenantId).size();
    }

    private PreservationExportFile toFile(Preservation aip) {
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
        if (aip.getSourceIntakeId() != null) data.put("sourceIntakeId", aip.getSourceIntakeId());
        if (aip.getRootElement() != null) {
            data.put("rootElement", ElementSerializer.serialize(aip.getRootElement()));
        }
        return new PreservationExportFile(aip.getId(), aip.getTitle(), data);
    }
}
