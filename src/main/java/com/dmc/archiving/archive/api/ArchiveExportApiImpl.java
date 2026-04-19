package com.dmc.archiving.archive.api;

import com.dmc.archiving.archive.ArchiveService;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.ArchiveStrategy;
import com.dmc.archiving.archive.strategy.ArchiveStrategyFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
class ArchiveExportApiImpl implements ArchiveExportApi {

    private final ArchiveService archiveService;
    private final ArchiveStrategyFactory strategyFactory;

    ArchiveExportApiImpl(ArchiveService archiveService, ArchiveStrategyFactory strategyFactory) {
        this.archiveService = archiveService;
        this.strategyFactory = strategyFactory;
    }

    @Override
    public ArchiveExportInfo loadForExport(Long archiveId) {
        Archive archive = archiveService.getArchiveForExport(archiveId);
        if (archive == null) {
            return null;
        }
        ArchiveStrategy strategy = strategyFactory.getStrategy(archive.getStandard());
        Map<String, Object> data = strategy.export(archive);
        return new ArchiveExportInfo(
                archive.getId(),
                archive.getTenantId(),
                archive.getTitle(),
                archive.getStandard().name(),
                strategy.getStandardName(),
                data
        );
    }
}
