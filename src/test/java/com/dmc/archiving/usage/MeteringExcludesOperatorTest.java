package com.dmc.archiving.usage;

import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.aip.model.AipStatus;
import com.dmc.archiving.aip.repository.AipRepository;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.dip.model.Dip;
import com.dmc.archiving.dip.model.DipStatus;
import com.dmc.archiving.dip.repository.DipRepository;
import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.document.model.DocumentStatus;
import com.dmc.archiving.document.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the meters exclude ADMIN/operator-created rows (billable = false),
 * so operator activity never lands on a tenant's bill (Risk 2c).
 */
@DataJpaTest
class MeteringExcludesOperatorTest {

    private static final Long TENANT = 100L;
    private static final Set<ArchiveStandard> PREMIUM =
            Set.of(ArchiveStandard.NOARK5, ArchiveStandard.EARK);

    @Autowired private DocumentRepository documents;
    @Autowired private AipRepository aips;
    @Autowired private DipRepository dips;

    private Document doc(long size, boolean billable) {
        Document d = new Document();
        d.setTitle("t"); d.setFileName("f");
        d.setFileKey("k-" + size + "-" + billable + "-" + System.nanoTime());
        d.setFileSize(size); d.setUserId(1L); d.setTenantId(TENANT);
        d.setStatus(DocumentStatus.ACTIVE); d.setCreatedAt(LocalDateTime.now());
        d.setBillable(billable);
        return d;
    }

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
    void storageSumExcludesOperatorUploads() {
        documents.save(doc(1000L, true));   // tenant upload
        documents.save(doc(250L, true));    // tenant upload
        documents.save(doc(9999L, false));  // ADMIN/operator upload — excluded

        assertThat(documents.sumFileSizeByTenantId(TENANT)).isEqualTo(1250L);
    }

    @Test
    void premiumPackageCountExcludesOperatorCreatedAipsAndDips() {
        aips.save(aip(ArchiveStandard.NOARK5, true));    // counts
        aips.save(aip(ArchiveStandard.EARK, false));     // ADMIN-created — excluded
        dips.save(dip(ArchiveStandard.NOARK5, true));    // counts
        dips.save(dip(ArchiveStandard.NOARK5, false));   // ADMIN-created — excluded

        assertThat(aips.countByTenantIdAndStandardInAndBillableTrue(TENANT, PREMIUM)).isEqualTo(1L);
        assertThat(dips.countByTenantIdAndStandardInAndBillableTrue(TENANT, PREMIUM)).isEqualTo(1L);
    }
}
