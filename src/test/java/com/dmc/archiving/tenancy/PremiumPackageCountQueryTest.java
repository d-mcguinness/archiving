package com.dmc.archiving.tenancy;

import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.aip.model.AipStatus;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.aip.repository.AipRepository;
import com.dmc.archiving.dip.model.Dip;
import com.dmc.archiving.dip.model.DipStatus;
import com.dmc.archiving.dip.repository.DipRepository;
import com.dmc.archiving.tenancy.repository.PremiumPackageUsageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the native combined premium-count read-model (Risk 3a-pkg): counts
 * billable NOARK5/E-ARK AIPs and DIPs by tenant, excluding non-premium and
 * operator (non-billable) rows. Confirms the native SQL matches the entity
 * tables/columns.
 */
@DataJpaTest
class PremiumPackageCountQueryTest {

    private static final Long TENANT = 100L;
    private static final List<String> PREMIUM = List.of("NOARK5", "EARK");

    @Autowired private AipRepository aips;
    @Autowired private DipRepository dips;
    @Autowired private PremiumPackageUsageRepository usage;

    private Aip aip(ArchiveStandard std, boolean billable) {
        Aip a = new Aip();
        a.setTenantId(TENANT); a.setOwnerId(1L); a.setTitle("a");
        a.setCreatedAt(LocalDateTime.now()); a.setStatus(AipStatus.STORED);
        a.setStandard(std); a.setBillable(billable);
        return a;
    }

    private Dip dip(ArchiveStandard std, boolean billable) {
        Dip d = new Dip();
        d.setTenantId(TENANT); d.setOwnerId(1L); d.setTitle("d");
        d.setCreatedAt(LocalDateTime.now()); d.setStatus(DipStatus.DISSEMINATED);
        d.setStandard(std); d.setBillable(billable);
        return d;
    }

    @Test
    void countsBillablePremiumAipsAndDipsExcludingNonPremiumAndOperatorRows() {
        aips.save(aip(ArchiveStandard.NOARK5, true));   // counts
        aips.save(aip(ArchiveStandard.EARK, true));     // counts
        aips.save(aip(ArchiveStandard.OAIS, true));     // not premium
        aips.save(aip(ArchiveStandard.NOARK5, false));  // operator (non-billable)

        dips.save(dip(ArchiveStandard.NOARK5, true));   // counts
        dips.save(dip(ArchiveStandard.EARK, false));    // operator (non-billable)

        assertThat(usage.countBillablePremiumAips(TENANT, PREMIUM)).isEqualTo(2L);
        assertThat(usage.countBillablePremiumDips(TENANT, PREMIUM)).isEqualTo(1L);
    }

    @Test
    void countsAreZeroForATenantWithNoPremiumPackages() {
        assertThat(usage.countBillablePremiumAips(404L, PREMIUM)).isEqualTo(0L);
        assertThat(usage.countBillablePremiumDips(404L, PREMIUM)).isEqualTo(0L);
    }
}
