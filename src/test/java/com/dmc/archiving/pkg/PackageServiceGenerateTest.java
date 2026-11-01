package com.dmc.archiving.pkg;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.pkg.generator.PackageGenerator;
import com.dmc.archiving.pkg.generator.PackageSnapshot;
import com.dmc.archiving.pkg.model.ArchivalPackage;
import com.dmc.archiving.pkg.model.PackageStage;
import com.dmc.archiving.pkg.model.PackageStatus;
import com.dmc.archiving.pkg.repository.PackageRepository;
import com.dmc.archiving.user.api.UserApi;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The unified package module generates through the same per-standard
 * generators the stage modules use, handing them an entity-free snapshot.
 */
class PackageServiceGenerateTest {

    private final PackageRepository repo = mock(PackageRepository.class);
    private final PackageGeneratorRegistry registry = mock(PackageGeneratorRegistry.class);
    @SuppressWarnings("unchecked")
    private final PackageGenerator<Object> generator = mock(PackageGenerator.class);
    private final PackageService service =
            new PackageService(repo, mock(UserApi.class), mock(EntityManager.class), registry);

    private static ArchivalPackage pkg() {
        ArchivalPackage p = new ArchivalPackage();
        p.setId(42L);
        p.setTenantId(1L);
        p.setOwnerId(1L);
        p.setTitle("Quarterly export");
        p.setDescription("desc");
        p.setStage(PackageStage.PRESERVATION);
        p.setStatus(PackageStatus.DRAFT);
        p.setStandard(ArchiveStandard.EARK);
        p.setSourcePackageId(17L);
        p.setCreatedAt(LocalDateTime.of(2026, 3, 1, 12, 0));
        Element root = new Element();
        root.setElementIdentifier("ARK-1");
        root.setEntityName("arkiv");
        root.setCreatedBy("bob");
        root.addField(new Field(null, null, "tittel", "Title", "string", "Q1"));
        p.setRootElement(root);
        return p;
    }

    @Test
    void generateDispatchesByStageAndStandardWithASnapshot() {
        when(repo.findById(42L)).thenReturn(Optional.of(pkg()));
        when(registry.getGenerator(PackageStage.PRESERVATION, ArchiveStandard.EARK)).thenAnswer(i -> generator);
        when(generator.generate(any(PackageSnapshot.class))).thenReturn("s3://bucket/aips/42/eark_aip.json");

        String url = service.generatePackage(42L);

        assertThat(url).isEqualTo("s3://bucket/aips/42/eark_aip.json");
        ArgumentCaptor<PackageSnapshot> captor = ArgumentCaptor.forClass(PackageSnapshot.class);
        verify(generator).generate(captor.capture());
        PackageSnapshot s = captor.getValue();
        assertThat(s.id()).isEqualTo(42L);
        assertThat(s.title()).isEqualTo("Quarterly export");
        assertThat(s.status()).isEqualTo("DRAFT");
        assertThat(s.standard()).isEqualTo("EARK");
        assertThat(s.sourceId()).isEqualTo(17L);
        assertThat(s.createdAt()).isEqualTo("2026-03-01T12:00:00");
        assertThat(s.elementIdentifier()).isEqualTo("ARK-1");
        assertThat(s.elementCreatedBy()).isEqualTo("bob");
        assertThat(s.fields()).containsEntry("tittel", "Q1");
        verify(generator, never()).generate(any(Object.class));
    }

    @Test
    void generateRejectsUnknownPackage() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generatePackage(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }
}
