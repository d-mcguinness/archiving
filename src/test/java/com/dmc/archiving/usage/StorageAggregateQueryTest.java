package com.dmc.archiving.usage;

import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.document.model.DocumentStatus;
import com.dmc.archiving.document.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the per-tenant storage meter is a real SQL aggregate (SUM over
 * file_size scoped by tenant_id), not in-memory counting (Risk 1d).
 */
@DataJpaTest
class StorageAggregateQueryTest {

    @Autowired
    private DocumentRepository repo;

    private Document doc(Long tenantId, long size) {
        Document d = new Document();
        d.setTitle("t");
        d.setFileName("f");
        d.setFileKey("k-" + tenantId + "-" + size + "-" + System.nanoTime());
        d.setFileSize(size);
        d.setUserId(1L);
        d.setTenantId(tenantId);
        d.setStatus(DocumentStatus.ACTIVE);
        d.setCreatedAt(LocalDateTime.now());
        return d;
    }

    @Test
    void sumsFileSizePerTenant_andIsolatesByTenant() {
        repo.save(doc(1L, 100L));
        repo.save(doc(1L, 250L));
        repo.save(doc(2L, 999L));

        assertThat(repo.sumFileSizeByTenantId(1L)).isEqualTo(350L);
        assertThat(repo.sumFileSizeByTenantId(2L)).isEqualTo(999L);
    }

    @Test
    void returnsZeroForTenantWithNoDocuments() {
        // COALESCE guards against SUM-over-empty returning null.
        assertThat(repo.sumFileSizeByTenantId(404L)).isEqualTo(0L);
    }
}
