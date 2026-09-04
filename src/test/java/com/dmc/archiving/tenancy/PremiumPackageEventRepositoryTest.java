package com.dmc.archiving.tenancy;

import com.dmc.archiving.tenancy.api.PremiumPackageType;
import com.dmc.archiving.tenancy.model.PremiumPackageEvent;
import com.dmc.archiving.tenancy.repository.PremiumPackageEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the append-only premium-package event ledger counts (metering
 * integrity): a lifetime count for the spend-cap guard, and a half-open
 * [start, end) window count for the per-period billing meter. Uses an isolated
 * tenant id so it is independent of any seeded ledger rows. Also asserts the
 * data.sql seed backfill statements (Review M3).
 */
@DataJpaTest
class PremiumPackageEventRepositoryTest {

    private static final Long TENANT = 9001L;
    private static final Long OTHER_TENANT = 9999L;

    @Autowired private PremiumPackageEventRepository events;

    private void record(Long tenantId, PremiumPackageType type, String standard, LocalDateTime at) {
        events.save(new PremiumPackageEvent(tenantId, type, standard, at));
    }

    @Test
    void lifetimeCountIsPerTenant() {
        record(TENANT, PremiumPackageType.PRESERVATION, "NOARK5", LocalDateTime.of(2026, 6, 5, 9, 0));
        record(TENANT, PremiumPackageType.RELEASE, "NOARK5", LocalDateTime.of(2026, 6, 5, 10, 0));
        record(TENANT, PremiumPackageType.PRESERVATION, "EARK", LocalDateTime.of(2026, 6, 6, 9, 0));
        record(OTHER_TENANT, PremiumPackageType.PRESERVATION, "NOARK5", LocalDateTime.of(2026, 6, 5, 9, 0));

        assertThat(events.countByTenantId(TENANT)).isEqualTo(3L);
        assertThat(events.countByTenantId(OTHER_TENANT)).isEqualTo(1L);
    }

    @Test
    void perPeriodWindowIsHalfOpenAndPerTenant() {
        record(TENANT, PremiumPackageType.PRESERVATION, "NOARK5", LocalDateTime.of(2026, 6, 5, 9, 0));
        record(TENANT, PremiumPackageType.RELEASE, "NOARK5", LocalDateTime.of(2026, 6, 5, 23, 59));
        record(TENANT, PremiumPackageType.PRESERVATION, "EARK", LocalDateTime.of(2026, 6, 6, 0, 0)); // boundary: next period
        record(OTHER_TENANT, PremiumPackageType.PRESERVATION, "NOARK5", LocalDateTime.of(2026, 6, 5, 12, 0));

        LocalDateTime day5Start = LocalDateTime.of(2026, 6, 5, 0, 0);
        LocalDateTime day6Start = LocalDateTime.of(2026, 6, 6, 0, 0);
        LocalDateTime day7Start = LocalDateTime.of(2026, 6, 7, 0, 0);

        // [June 5, June 6): the two June-5 events; the June-6 00:00 event is excluded (half-open).
        assertThat(events.countByTenantIdAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
                TENANT, day5Start, day6Start)).isEqualTo(2L);
        // [June 6, June 7): exactly the boundary event — billed in this period, not the previous one.
        assertThat(events.countByTenantIdAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
                TENANT, day6Start, day7Start)).isEqualTo(1L);
    }

    @Test
    @Sql("/sql/premium-events-backfill.sql") // mirrors src/main/resources/data.sql (Review M3)
    void seedBackfillProducesTwoHistoricalEventsForTenantOne() {
        // The backfill bridges the seeded SIP->AIP->DIP chain into the ledger; a
        // wrong column/value would seed 0 (under-bill) — assert it lands 2 events.
        assertThat(events.countByTenantId(1L)).isEqualTo(2L);
        // Stamped with a fixed historical date (Review L1), so they fall in Jan 2026
        // and never inflate the current billing period's count.
        assertThat(events.countByTenantIdAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
                1L, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 2, 1, 0, 0))).isEqualTo(2L);
    }
}
