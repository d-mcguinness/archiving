package com.dmc.archiving.usage;

import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.aip.model.AipStatus;
import com.dmc.archiving.aip.repository.AipRepository;
import com.dmc.archiving.archive.model.ArchiveStandard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the per-period premium count (Risk 3b-meter): only packages
 * GENERATED within the period window are counted, so a one-time generation is
 * billed once and a tenant with only historical packages is billed $0 this
 * period.
 */
@DataJpaTest
class PremiumPeriodCountTest {

    private static final Long TENANT = 100L;
    private static final List<ArchiveStandard> PREMIUM =
            List.of(ArchiveStandard.NOARK5, ArchiveStandard.EARK);

    @Autowired private AipRepository aips;

    private Aip premiumAip(LocalDateTime createdAt) {
        Aip a = new Aip();
        a.setTenantId(TENANT); a.setOwnerId(1L); a.setTitle("a");
        a.setStatus(AipStatus.STORED); a.setStandard(ArchiveStandard.NOARK5);
        a.setBillable(true); a.setCreatedAt(createdAt);
        return a;
    }

    @Test
    void countsOnlyPackagesGeneratedWithinThePeriod() {
        LocalDate period = LocalDate.of(2026, 6, 5);
        LocalDateTime start = period.atStartOfDay();
        LocalDateTime end = period.plusDays(1).atStartOfDay();

        aips.save(premiumAip(period.atTime(9, 0)));    // in period -> counts
        aips.save(premiumAip(period.atTime(23, 59)));  // in period -> counts
        aips.save(premiumAip(period.minusDays(1).atTime(12, 0))); // before -> excluded
        aips.save(premiumAip(end));                    // exactly next-day 00:00 -> excluded (half-open)

        long inPeriod = aips.countByTenantIdAndStandardInAndBillableTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                TENANT, PREMIUM, start, end);
        long lifetime = aips.countByTenantIdAndStandardInAndBillableTrue(TENANT, PREMIUM);

        assertThat(inPeriod).isEqualTo(2L);   // per-period FLOW
        assertThat(lifetime).isEqualTo(4L);   // cumulative (drives the lifetime cap, not the bill)
    }
}
