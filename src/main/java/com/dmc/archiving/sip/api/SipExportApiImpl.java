package com.dmc.archiving.sip.api;

import com.dmc.archiving.archive.element.ElementSerializer;
import com.dmc.archiving.sip.SipService;
import com.dmc.archiving.sip.model.Sip;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class SipExportApiImpl implements SipExportApi {

    private final SipService sipService;

    SipExportApiImpl(SipService sipService) {
        this.sipService = sipService;
    }

    @Override
    public List<SipExportFile> exportByTenant(Long tenantId) {
        return sipService.getSipsByTenant(tenantId).stream()
                .map(this::toFile)
                .toList();
    }

    @Override
    public int countByTenant(Long tenantId) {
        return sipService.getSipsByTenant(tenantId).size();
    }

    private SipExportFile toFile(Sip sip) {
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
        return new SipExportFile(sip.getId(), sip.getTitle(), data);
    }
}
