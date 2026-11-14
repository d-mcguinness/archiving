package com.dmc.archiving.release.api;

import com.dmc.archiving.archive.api.ElementTreeOwner;
import com.dmc.archiving.release.repository.ReleaseRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Resolves the tenant of an element tree that belongs to a release. */
@Component
class ReleaseElementTreeOwner implements ElementTreeOwner {

    private final ReleaseRepository repository;

    ReleaseElementTreeOwner(ReleaseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Long> tenantOfRootElement(Long rootElementId) {
        return repository.findTenantIdByRootElementId(rootElementId);
    }
}
