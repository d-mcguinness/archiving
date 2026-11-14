package com.dmc.archiving.preservation.api;

import com.dmc.archiving.archive.api.ElementTreeOwner;
import com.dmc.archiving.preservation.repository.PreservationRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Resolves the tenant of an element tree that belongs to a preservation. */
@Component
class PreservationElementTreeOwner implements ElementTreeOwner {

    private final PreservationRepository repository;

    PreservationElementTreeOwner(PreservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Long> tenantOfRootElement(Long rootElementId) {
        return repository.findTenantIdByRootElementId(rootElementId);
    }
}
