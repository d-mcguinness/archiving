package com.dmc.archiving.intake.api;

import com.dmc.archiving.archive.api.ElementTreeOwner;
import com.dmc.archiving.intake.repository.IntakeRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Resolves the tenant of an element tree that belongs to an intake. */
@Component
class IntakeElementTreeOwner implements ElementTreeOwner {

    private final IntakeRepository repository;

    IntakeElementTreeOwner(IntakeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Long> tenantOfRootElement(Long rootElementId) {
        return repository.findTenantIdByRootElementId(rootElementId);
    }
}
