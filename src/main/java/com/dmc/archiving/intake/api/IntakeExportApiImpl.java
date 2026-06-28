package com.dmc.archiving.intake.api;

import com.dmc.archiving.archive.element.ElementSerializer;
import com.dmc.archiving.intake.IntakeService;
import com.dmc.archiving.intake.model.Intake;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class IntakeExportApiImpl implements IntakeExportApi {

    private final IntakeService sipService;

    IntakeExportApiImpl(IntakeService sipService) {
        this.sipService = sipService;
    }

    @Override
    public List<IntakeExportFile> exportByTenant(Long tenantId) {
        return sipService.getIntakesByTenant(tenantId).stream()
                .map(this::toFile)
                .toList();
    }

    @Override
    public int countByTenant(Long tenantId) {
        return sipService.getIntakesByTenant(tenantId).size();
    }

    private IntakeExportFile toFile(Intake sip) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", sip.getId());
        data.put("stage", "SIP");
        data.put("title", sip.getTitle());
        data.put("description", sip.getDescription());
        data.put("standard", sip.getStandard().name());
        data.put("status", sip.getStatus().name());
        data.put("ownerId", sip.getOwnerId());
        data.put("tenantId", sip.getTenantId());
        data.put("createdAt", sip.getCreatedAt());
        data.put("updatedAt", sip.getUpdatedAt());
        if (sip.getRootElement() != null) {
            data.put("rootElement", ElementSerializer.serialize(sip.getRootElement()));
        }
        return new IntakeExportFile(sip.getId(), sip.getTitle(), data);
    }
}
