package com.dmc.archiving.archive.element;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.archive.model.ArchiveStatus;
import com.dmc.archiving.archive.repository.ArchiveRepository;
import com.dmc.archiving.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies ElementService.getArchiveTenantId resolves the owning archive's
 * tenant through the lazy association within its own transaction (Review
 * H1-elem) — the basis for element/link tenant-ownership checks.
 */
@DataJpaTest
@Import(ElementService.class)
class ElementArchiveTenantTest {

    @Autowired private ArchiveRepository archives;
    @Autowired private ElementRepository elements;
    @Autowired private ElementService elementService;

    private Archive archive(Long tenantId) {
        Archive a = new Archive();
        a.setTenantId(tenantId);
        a.setOwnerId(1L);
        a.setTitle("arch");
        a.setStatus(ArchiveStatus.DRAFT);
        a.setStandard(ArchiveStandard.NOARK5);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        return a;
    }

    private Element element(Archive archive) {
        Element e = new Element();
        e.setArchive(archive);
        e.setElementIdentifier("E1");
        e.setEntityName("Personnel");
        e.setEntityType("Arkivdel");
        e.setTitle("Records");
        e.setStatus("Opprettet");
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    @Test
    void resolvesTheOwningArchivesTenant() {
        Archive a = archives.save(archive(100L));
        Element e = elements.save(element(a));

        assertThat(elementService.getArchiveTenantId(e.getId())).isEqualTo(100L);
    }

    @Test
    void missingElementIsNotFound() {
        assertThatThrownBy(() -> elementService.getArchiveTenantId(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
