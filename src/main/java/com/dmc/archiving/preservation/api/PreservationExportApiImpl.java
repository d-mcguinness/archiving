package com.dmc.archiving.preservation.api;

import com.dmc.archiving.archive.element.ElementSerializer;
import com.dmc.archiving.pkg.generator.PackagePayload;
import com.dmc.archiving.pkg.generator.PackageSnapshot;
import com.dmc.archiving.preservation.PreservationService;
import com.dmc.archiving.preservation.generator.PreservationGeneratorFactory;
import com.dmc.archiving.preservation.model.Preservation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
class PreservationExportApiImpl implements PreservationExportApi {

    private final PreservationService aipService;
    private final PreservationGeneratorFactory generatorFactory;

    PreservationExportApiImpl(PreservationService aipService, PreservationGeneratorFactory generatorFactory) {
        this.aipService = aipService;
        this.generatorFactory = generatorFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PreservationExtract> extract(Long preservationId) {
        Preservation aip = aipService.getPreservation(preservationId);
        if (aip == null) return Optional.empty();
        PackageSnapshot snapshot = PackageSnapshot.of(
                aip.getId(), aip.getTitle(), aip.getDescription(),
                aip.getStatus(), aip.getStandard(),
                aip.getCreatedAtString(), aip.getUpdatedAtString(),
                aip.getSourceIntakeId(), aip.getRootElement());
        PackagePayload artifact = generatorFactory.getGenerator(aip.getStandard()).render(snapshot);
        return Optional.of(new PreservationExtract(
                aip.getId(), aip.getTitle(), aip.getTenantId(), aip.getStandard(),
                aip.getSourceIntakeId(), toFile(aip).data(), artifact));
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
