package com.dmc.archiving.scheme;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.scheme.api.EntityDefinition;
import com.dmc.archiving.scheme.api.FieldDefinition;
import com.dmc.archiving.scheme.api.FieldOption;
import com.dmc.archiving.scheme.api.SchemeDefinition;
import com.dmc.archiving.scheme.api.SchemeStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The bundled definitions are complete and self-consistent; a broken one fails fast. */
class SchemeDefinitionLoaderTest {

    private final SchemeDefinitionLoader loader = new SchemeDefinitionLoader();

    @Test
    void everyArchiveStandardHasExactlyOneDefinition() {
        assertThat(loader.all()).extracting(SchemeDefinition::standard)
                .containsExactlyInAnyOrder(ArchiveStandard.values());
        for (ArchiveStandard std : ArchiveStandard.values()) {
            assertThat(loader.get(std)).as(std.name()).isPresent();
        }
        assertThat(loader.get(null)).isEmpty();
    }

    @Test
    void everyDefinitionNamesARootEntityForEachStage() {
        for (SchemeDefinition def : loader.all()) {
            for (SchemeStage stage : SchemeStage.values()) {
                assertThat(def.stageEntity(stage)).as("%s %s", def.standard(), stage).isPresent();
                assertThat(loader.stageEntity(def.standard(), stage)).isEqualTo(def.stageEntity(stage));
            }
        }
    }

    @Test
    void everyFieldIsFullyTypedAndChildrenResolve() {
        Set<String> types = Set.of("string", "text", "date", "number");
        for (SchemeDefinition def : loader.all()) {
            assertThat(def.entities()).as(def.standard().name()).isNotEmpty();
            for (EntityDefinition e : def.entities()) {
                assertThat(e.name()).isNotBlank();
                for (FieldDefinition f : e.fields()) {
                    assertThat(f.name()).as("%s/%s", def.standard(), e.name()).isNotBlank();
                    assertThat(f.label()).isNotBlank();
                    assertThat(types).contains(f.type());
                }
                // Every declared child is itself an entity of the standard
                assertThat(def.childEntities(e.name())).hasSize(
                        (int) e.children().stream().filter(c -> def.entity(c).isPresent()).count());
            }
        }
    }

    @Test
    void noark5LooksLikeNoark5() {
        SchemeDefinition n5 = loader.get(ArchiveStandard.NOARK5).orElseThrow();
        assertThat(n5.displayName()).isEqualTo("NOARK5");
        assertThat(n5.fullName()).isEqualTo("Norsk Arkivstandard 5");
        assertThat(n5.stageEntity(SchemeStage.INTAKE).map(EntityDefinition::name)).contains("Archive");
        assertThat(n5.entity("Archive").orElseThrow().fields()).extracting(FieldDefinition::name)
                .contains("systemID", "title", "createdDate", "closedBy");
        assertThat(n5.vocabularies()).containsKey("statusValues").doesNotContainKey("entities");
        assertThat(n5.childEntities("Archive")).extracting(EntityDefinition::name).contains("Series");
    }

    @Test
    void enumeratedFieldsResolveToTheirVocabulary() {
        SchemeDefinition n5 = loader.get(ArchiveStandard.NOARK5).orElseThrow();
        FieldDefinition status = n5.entity("Archive").orElseThrow().fields().stream()
                .filter(f -> f.name().equals("archiveStatus")).findFirst().orElseThrow();
        assertThat(status.isEnumerated()).isTrue();
        assertThat(status.vocabulary()).isEqualTo("statusValues.archiveStatus");
        assertThat(status.options()).extracting(FieldOption::value)
                .containsExactly("Created", "Closed", "Transferred", "Periodized", "Deleted");

        // Object vocabularies: keys are the values, descriptions become labels
        FieldDefinition medium = n5.entity("Archive").orElseThrow().fields().stream()
                .filter(f -> f.name().equals("documentMedium")).findFirst().orElseThrow();
        assertThat(medium.options()).extracting(FieldOption::value)
                .contains("Electronic archive", "Physical medium");
        assertThat(medium.options()).filteredOn(o -> o.value().startsWith("Mixed"))
                .extracting(FieldOption::label).first().asString().contains("mix");

        // Free-text fields carry no vocabulary
        FieldDefinition title = n5.entity("Archive").orElseThrow().fields().stream()
                .filter(f -> f.name().equals("title")).findFirst().orElseThrow();
        assertThat(title.isEnumerated()).isFalse();
        assertThat(title.vocabulary()).isNull();

        long enumerated = loader.all().stream().flatMap(d -> d.entities().stream())
                .flatMap(e -> e.fields().stream()).filter(FieldDefinition::isEnumerated).count();
        assertThat(enumerated).as("fields linked to a vocabulary across all standards").isGreaterThan(90);
    }

    @Test
    void definitionsAreImmutable() {
        SchemeDefinition def = loader.all().get(0);
        assertThatThrownBy(() -> def.entities().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> def.vocabularies().put("x", 1)).isInstanceOf(UnsupportedOperationException.class);
        List<SchemeDefinition> all = loader.all();
        assertThatThrownBy(() -> all.add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void failsFastWhenNothingIsFound() {
        assertThatThrownBy(() -> new SchemeDefinitionLoader(new ObjectMapper(), "classpath*:no-such-dir/*.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No scheme definitions");
    }
}
