package com.dmc.archiving.archive.api;

import com.dmc.archiving.archive.repository.ArchiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ArchiveAccessApiImpl implements ArchiveAccessApi {

    private final ArchiveRepository archiveRepository;

    ArchiveAccessApiImpl(ArchiveRepository archiveRepository) {
        this.archiveRepository = archiveRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ArchiveAccessInfo accessFor(Long archiveId, Long userId) {
        if (archiveId == null) {
            return null;
        }
        return archiveRepository.findById(archiveId)
                .map(archive -> new ArchiveAccessInfo(
                        archive.getId(),
                        archive.getTenantId(),
                        userId != null && archiveRepository.isOwnerOrAssigned(archiveId, userId)))
                .orElse(null);
    }
}
