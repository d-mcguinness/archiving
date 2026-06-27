package com.dmc.archiving.billing.service;

import org.junit.jupiter.api.Test;

import static com.dmc.archiving.billing.service.BillingMeterReportService.gbMonthMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The storage STOCK→FLOW conversion is the single hardest correctness problem in
 * billing: daily point-in-time bytes must integrate to GB-months, not be summed
 * into a sum meter (which would over-count by ~the number of days). These cases
 * pin the exact integer (round-half-up) milli-GB-month math.
 *
 * <p>Quantity is milli-GB-months (GB-months × 1000); the Stripe per-unit price is
 * the GB-month rate ÷ 1000, so 1000 quantity × ($0.18/1000) = $0.18 for 1 GB-month.
 */
class BillingMeterMathTest {

    private static final long GB = 1_000_000_000L;

    @Test
    void fullPeriodAtOneGbIsExactlyOneGbMonth() {
        // 1 GB held every day of a 30-day period → 1.000 GB-month → 1000 milli.
        assertThat(gbMonthMillis(30 * GB, 30)).isEqualTo(1000);
    }

    @Test
    void aFullMonthIsOneGbMonthRegardlessOfMonthLength() {
        // 28-day Feb and 31-day month both yield 1.000 GB-month for 1 GB held throughout.
        assertThat(gbMonthMillis(28 * GB, 28)).isEqualTo(1000);
        assertThat(gbMonthMillis(31 * GB, 31)).isEqualTo(1000);
    }

    @Test
    void partialOccupancyProratesWithinThePeriod() {
        // 1 GB held only 15 of 30 days → 0.5 GB-month → 500 milli.
        assertThat(gbMonthMillis(15 * GB, 30)).isEqualTo(500);
    }

    @Test
    void tenGbForAFullMonthIsTenGbMonths() {
        assertThat(gbMonthMillis(30 * (10 * GB), 30)).isEqualTo(10_000); // 10.000 GB-months
    }

    @Test
    void emptyUsageIsZero() {
        assertThat(gbMonthMillis(0, 31)).isEqualTo(0);
    }

    @Test
    void roundsHalfUp() {
        // days=1 → divisor 1_000_000; 1_500_000 byte-days = 1.5 milli → rounds to 2.
        assertThat(gbMonthMillis(1_500_000L, 1)).isEqualTo(2);
        // 1_499_999 = 1.499999 milli → rounds to 1.
        assertThat(gbMonthMillis(1_499_999L, 1)).isEqualTo(1);
        // exactly 1.0 milli.
        assertThat(gbMonthMillis(1_000_000L, 1)).isEqualTo(1);
    }

    @Test
    void petabyteScaleDoesNotOverflow() {
        long onePetabyte = 1_000_000_000_000_000L; // 1e15 bytes
        // 1 PB held every day for 31 days → 1,000,000 GB-months → 1e9 milli.
        assertThat(gbMonthMillis(31 * onePetabyte, 31)).isEqualTo(1_000_000_000L);
    }

    @Test
    void rejectsNonPositiveDays() {
        assertThatThrownBy(() -> gbMonthMillis(GB, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gbMonthMillis(GB, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeByteDays() {
        assertThatThrownBy(() -> gbMonthMillis(-1, 30)).isInstanceOf(IllegalArgumentException.class);
    }
}
