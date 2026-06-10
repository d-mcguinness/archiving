package com.dmc.archiving.tenancy;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.tenancy.api.PremiumStandards;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Guards the single-source premium-standard set (Review dedup). The canonical
 * set lives in the tenancy module as enum NAMES (the billing module owns the
 * "premium" concept, and tenancy cannot import the archive enum without a module
 * cycle). These assertions protect the string-to-{@code ArchiveStandard.name()}
 * contract: the names are persisted in the {@code premium_package_events.standard}
 * (and {@code aips/dips.standard}) EnumType.STRING columns, so a name that didn't
 * match a real enum constant would silently fail to match stored rows.
 */
class PremiumStandardsTest {

    @Test
    void everyNameMapsToARealArchiveStandard() {
        // Each NAME must equal an ArchiveStandard.name() so it matches the persisted
        // EnumType.STRING value; a typo would throw here.
        assertThatCode(() -> PremiumStandards.NAMES.forEach(ArchiveStandard::valueOf))
                .doesNotThrowAnyException();
    }

    @Test
    void resolvesToExactlyNoark5AndEark() {
        Set<ArchiveStandard> resolved = PremiumStandards.NAMES.stream()
                .map(ArchiveStandard::valueOf)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(resolved).containsExactlyInAnyOrder(ArchiveStandard.NOARK5, ArchiveStandard.EARK);
    }

    @Test
    void containsIsFailSafeForUnknownNames() {
        assertThat(PremiumStandards.contains("NOARK5")).isTrue();
        assertThat(PremiumStandards.contains("EARK")).isTrue();
        assertThat(PremiumStandards.contains("OAIS")).isFalse();
        assertThat(PremiumStandards.contains("nonsense")).isFalse();
        assertThat(PremiumStandards.contains("noark5")).isFalse(); // case-sensitive: must match enum name exactly
    }
}
