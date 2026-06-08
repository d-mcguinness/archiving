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
 * cycle). These assertions ensure the names stay in lockstep with the real
 * ArchiveStandard enum, so the usage module's name->enum derivation can't throw.
 */
class PremiumStandardsTest {

    @Test
    void everyNameMapsToARealArchiveStandard() {
        // Mirrors UsageAggregationService's static derivation; a typo would throw here.
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
