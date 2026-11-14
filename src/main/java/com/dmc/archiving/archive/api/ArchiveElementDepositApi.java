package com.dmc.archiving.archive.api;

import java.util.List;

/**
 * Lets another module add described content to an archive's element tree
 * without holding the element-editing role: the caller has already decided
 * the user may put content into this archive (see {@link ArchiveAccessApi}),
 * and the archive decides where each element goes.
 */
public interface ArchiveElementDepositApi {

    /**
     * Adds the elements to the archive, in order, and returns their ids.
     *
     * <p>Each element is placed under the nearest fitting parent: an element
     * added earlier in the same call for its {@code parentEntityName}, else the
     * archive's most recent existing element of that entity, else the archive's
     * root element (and at the top level when the archive has none).
     *
     * @throws IllegalArgumentException if the archive does not exist
     */
    List<Long> deposit(Long archiveId, List<ElementDeposit> elements, String createdBy);

    /**
     * Removes what was deposited with exactly this {@link ElementDeposit#metadata()}
     * from the tenant's archives — whichever archive it went into — and returns
     * how many deposited elements were removed. Anything nested beneath them and
     * any links to or from them go too.
     */
    int removeDeposited(Long tenantId, String metadata);
}
