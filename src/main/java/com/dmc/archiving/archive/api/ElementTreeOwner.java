package com.dmc.archiving.archive.api;

import java.util.Optional;

/**
 * Implemented by modules whose records own an element tree that is not attached
 * to an {@code Archive} — intakes, preservations and releases keep their own
 * root element. Lets the archive module resolve the tenant of such an element
 * without depending on those modules.
 */
public interface ElementTreeOwner {

    /** Tenant of this module's record whose root element is {@code rootElementId}, if it owns one. */
    Optional<Long> tenantOfRootElement(Long rootElementId);
}
