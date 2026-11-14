package com.dmc.archiving.archive.element;

import com.dmc.archiving.archive.api.ArchiveElementDepositApi;
import com.dmc.archiving.archive.api.ElementDeposit;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.archive.model.ArchiveStatus;
import com.dmc.archiving.archive.repository.ArchiveRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Where deposited elements land in an archive's tree: under an element added
 * in the same deposit, else under the archive's latest element of the parent
 * entity, else under the archive's root.
 */
// `fields.value` is an H2 keyword, so the default slice database cannot create or read
// the fields table (which deleting an element cascades into). Allow it as an identifier.
@DataJpaTest(properties = {
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:h2:mem:element-deposit;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(ElementService.class)
@ComponentScan(basePackages = "com.dmc.archiving.archive.api", useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ArchiveElementDepositApi.class))
class ArchiveElementDepositTest {

    @Autowired private ArchiveRepository archives;
    @Autowired private ElementRepository elements;
    @Autowired private ArchiveElementDepositApi depositApi;
    @Autowired private jakarta.persistence.EntityManager entityManager;
    @Autowired private com.dmc.archiving.archive.element.link.ElementLinkRepository links;

    private Archive archiveWithRoot() {
        Archive a = new Archive();
        a.setTenantId(1L);
        a.setOwnerId(1L);
        a.setTitle("arch");
        a.setStatus(ArchiveStatus.DRAFT);
        a.setStandard(ArchiveStandard.NOARK5);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        a = archives.save(a);
        a.setRootElement(existing(a, null, "Archive"));
        return archives.save(a);
    }

    private Element existing(Archive archive, Element parent, String entityName) {
        Element e = new Element();
        e.setArchive(archive);
        e.setParent(parent);
        e.setElementIdentifier(entityName + "-" + System.nanoTime());
        e.setEntityName(entityName);
        e.setEntityType("structural_unit");
        e.setTitle(entityName);
        e.setStatus("Opprettet");
        e.setCreatedAt(LocalDateTime.now());
        e.setIsRoot(parent == null);
        return elements.save(e);
    }

    private static ElementDeposit deposit(String entity, String parentEntity) {
        return new ElementDeposit(entity, "record_unit", parentEntity, "DOC-1-" + entity, "Report",
                "{\"documentId\":1}",
                List.of(new ElementDeposit.Field("title", "Title", "string", "Report")));
    }

    @Test
    void nestsUnderAnElementAddedEarlierInTheSameDeposit() {
        Archive archive = archiveWithRoot();

        List<Long> ids = depositApi.deposit(archive.getId(), List.of(
                deposit("Registration", "File"),
                deposit("Document Description", "Registration")), "user");

        Element registration = elements.findById(ids.get(0)).orElseThrow();
        Element description = elements.findById(ids.get(1)).orElseThrow();
        assertThat(description.getParent().getId()).isEqualTo(registration.getId());
        assertThat(description.getArchive().getId()).isEqualTo(archive.getId());
        assertThat(description.getCreatedBy()).isEqualTo("user");
        assertThat(description.getMetadata()).isEqualTo("{\"documentId\":1}");
        assertThat(description.getFields()).hasSize(1);
        assertThat(description.getFields().get(0).getValue()).isEqualTo("Report");
    }

    @Test
    void attachesToTheArchivesExistingElementOfTheParentEntity() {
        Archive archive = archiveWithRoot();
        Element file = existing(archive, archive.getRootElement(), "File");

        List<Long> ids = depositApi.deposit(archive.getId(), List.of(deposit("Registration", "File")), "user");

        assertThat(elements.findById(ids.get(0)).orElseThrow().getParent().getId()).isEqualTo(file.getId());
    }

    @Test
    void fallsBackToTheArchiveRoot() {
        Archive archive = archiveWithRoot();

        List<Long> ids = depositApi.deposit(archive.getId(), List.of(
                deposit("Registration", "File"),   // no File in this archive
                deposit("Event", null)), "user");  // top-level entity in the standard

        Long rootId = archive.getRootElement().getId();
        assertThat(elements.findById(ids.get(0)).orElseThrow().getParent().getId()).isEqualTo(rootId);
        assertThat(elements.findById(ids.get(1)).orElseThrow().getParent().getId()).isEqualTo(rootId);
        assertThat(elements.findById(ids.get(1)).orElseThrow().getIsRoot()).isFalse();
    }

    @Test
    void doesNotAttachToAnotherArchivesElement() {
        Archive other = archiveWithRoot();
        existing(other, other.getRootElement(), "File");
        Archive archive = archiveWithRoot();

        List<Long> ids = depositApi.deposit(archive.getId(), List.of(deposit("Registration", "File")), "user");

        assertThat(elements.findById(ids.get(0)).orElseThrow().getParent().getId())
                .isEqualTo(archive.getRootElement().getId());
    }

    private static ElementDeposit depositFor(long documentId, String entity, String parentEntity) {
        return new ElementDeposit(entity, "record_unit", parentEntity, "DOC-" + documentId + "-" + entity, "Report",
                "{\"documentId\":" + documentId + "}",
                List.of(new ElementDeposit.Field("title", "Title", "string", "Report")));
    }

    @Test
    void removesOnlyWhatWasDepositedForThatDocument() {
        Archive archive = archiveWithRoot();
        Element file = existing(archive, archive.getRootElement(), "File");
        List<Long> doc1 = depositApi.deposit(archive.getId(), List.of(
                depositFor(1, "Registration", "File"),
                depositFor(1, "Document Description", "Registration"),
                depositFor(1, "Event", null)), "user");
        List<Long> doc2 = depositApi.deposit(archive.getId(), List.of(depositFor(2, "Registration", "File")), "user");
        flushAndClear();

        int removed = depositApi.removeDeposited(1L, "{\"documentId\":1}");
        flushAndClear();

        assertThat(removed).isEqualTo(3);
        assertThat(elements.findAllById(doc1)).isEmpty();
        assertThat(elements.findById(doc2.get(0))).isPresent();
        assertThat(elements.findById(file.getId())).isPresent();
        assertThat(elements.findById(archive.getRootElement().getId())).isPresent();
    }

    @Test
    void removesLinksPointingAtDepositedElements() {
        Archive archive = archiveWithRoot();
        Element file = existing(archive, archive.getRootElement(), "File");
        List<Long> ids = depositApi.deposit(archive.getId(), List.of(depositFor(1, "Registration", "File")), "user");
        com.dmc.archiving.archive.element.link.ElementLink link = new com.dmc.archiving.archive.element.link.ElementLink();
        link.setSourceElement(file);
        link.setTargetElement(elements.findById(ids.get(0)).orElseThrow());
        link.setLinkType("references");
        link.setDirectional(true);
        link.setCreatedAt(LocalDateTime.now());
        links.save(link);
        flushAndClear();

        depositApi.removeDeposited(1L, "{\"documentId\":1}");
        flushAndClear();

        assertThat(elements.findById(ids.get(0))).isEmpty();
        assertThat(links.findAll()).isEmpty();
        assertThat(elements.findById(file.getId())).isPresent();
    }

    @Test
    void removalDoesNotReachAnotherTenantsArchives() {
        Archive archive = archiveWithRoot(); // tenant 1
        List<Long> ids = depositApi.deposit(archive.getId(), List.of(depositFor(1, "Registration", "File")), "user");
        flushAndClear();

        assertThat(depositApi.removeDeposited(2L, "{\"documentId\":1}")).isZero();
        assertThat(depositApi.removeDeposited(null, "{\"documentId\":1}")).isZero();
        assertThat(depositApi.removeDeposited(1L, " ")).isZero();
        flushAndClear();

        assertThat(elements.findById(ids.get(0))).isPresent();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void unknownArchiveIsRejected() {
        assertThatThrownBy(() -> depositApi.deposit(987654L, List.of(deposit("Registration", "File")), "user"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
