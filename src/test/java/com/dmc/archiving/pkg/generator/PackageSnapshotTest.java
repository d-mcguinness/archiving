package com.dmc.archiving.pkg.generator;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.pkg.model.PackageStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PackageSnapshotTest {

    private static Element rootWithFields() {
        Element e = new Element();
        e.setElementIdentifier("ARK-001");
        e.setEntityName("arkiv");
        e.setEntityType("Arkiv");
        e.setNorwegianName("Arkiv");
        e.setEnglishName("Fonds");
        e.setTitle("Root");
        e.setDescription("Root desc");
        e.setCreatedBy("alice");
        e.setCreatedAt(LocalDateTime.of(2026, 1, 2, 3, 4));
        e.setStatus("Created");
        e.addField(new Field(null, null, "tittel", "Title", "string", "Employee Records"));
        e.addField(new Field(null, null, "arkivstatus", "Status", "enum", "Opprettet"));
        return e;
    }

    @Test
    void ofCapturesEnvelopeAndFlattensRootElement() {
        PackageSnapshot s = PackageSnapshot.of(7L, "T", "D", PackageStatus.DRAFT, ArchiveStandard.NOARK5,
                "2026-01-01T00:00:00", "2026-01-02T00:00:00", 3L, rootWithFields());

        assertThat(s.id()).isEqualTo(7L);
        assertThat(s.status()).isEqualTo("DRAFT");
        assertThat(s.standard()).isEqualTo("NOARK5");
        assertThat(s.sourceId()).isEqualTo(3L);
        assertThat(s.hasRootElement()).isTrue();
        assertThat(s.hasFields()).isTrue();
        assertThat(s.fields()).containsExactly(
                java.util.Map.entry("tittel", "Employee Records"),
                java.util.Map.entry("arkivstatus", "Opprettet"));
        assertThat(s.fieldList()).extracting(PackageSnapshot.FieldSnapshot::label).containsExactly("Title", "Status");
        assertThat(s.elementCreatedAt()).isEqualTo("2026-01-02T03:04");
        assertThat(s.elementCreatedBy()).isEqualTo("alice");
    }

    @Test
    void ofToleratesMissingRootAndNullEnums() {
        PackageSnapshot s = PackageSnapshot.of(1L, "T", null, null, null, null, null, null, null);

        assertThat(s.hasRootElement()).isFalse();
        assertThat(s.hasFields()).isFalse();
        assertThat(s.status()).isNull();
        assertThat(s.standard()).isEqualTo("UNKNOWN");
        assertThat(s.toElement()).isNull();
    }

    @Test
    void toElementRebuildsRootWithFieldsIntact() {
        PackageSnapshot s = PackageSnapshot.of(7L, "T", "D", PackageStatus.DRAFT, ArchiveStandard.NOARK5,
                null, null, null, rootWithFields());

        Element rebuilt = s.toElement();

        assertThat(rebuilt).isNotNull();
        assertThat(rebuilt.getIsRoot()).isTrue();
        assertThat(rebuilt.getElementIdentifier()).isEqualTo("ARK-001");
        assertThat(rebuilt.getNorwegianName()).isEqualTo("Arkiv");
        assertThat(rebuilt.getFields()).hasSize(2);
        Field f = rebuilt.getFields().get(1);
        assertThat(f.getName()).isEqualTo("arkivstatus");
        assertThat(f.getLabel()).isEqualTo("Status");
        assertThat(f.getType()).isEqualTo("enum");
        assertThat(f.getValue()).isEqualTo("Opprettet");
        assertThat(f.getElement()).isSameAs(rebuilt);
    }

    @Test
    void collectionsAreDefensiveAndUnmodifiable() {
        PackageSnapshot s = PackageSnapshot.of(1L, "T", null, null, null, null, null, null, rootWithFields());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> s.fields().put("x", "y"));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> s.fieldList().add(null));
    }
}
