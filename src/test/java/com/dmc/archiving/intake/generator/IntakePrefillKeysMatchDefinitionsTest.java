package com.dmc.archiving.intake.generator;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.intake.generator.impl.BagitIntakeGenerator;
import com.dmc.archiving.intake.generator.impl.DublinCoreIntakeGenerator;
import com.dmc.archiving.intake.generator.impl.EadIntakeGenerator;
import com.dmc.archiving.intake.generator.impl.EarkIntakeGenerator;
import com.dmc.archiving.intake.generator.impl.IsadgIntakeGenerator;
import com.dmc.archiving.intake.generator.impl.MetsIntakeGenerator;
import com.dmc.archiving.intake.generator.impl.ModsIntakeGenerator;
import com.dmc.archiving.intake.generator.impl.Noark5IntakeGenerator;
import com.dmc.archiving.intake.generator.impl.OaisIntakeGenerator;
import com.dmc.archiving.intake.generator.impl.PremisIntakeGenerator;
import com.dmc.archiving.intake.input.FileMetadataInput;
import com.dmc.archiving.scheme.SchemeDefinitionLoader;
import com.dmc.archiving.scheme.api.EntityDefinition;
import com.dmc.archiving.scheme.api.FieldDefinition;
import com.dmc.archiving.scheme.api.SchemeStage;
import com.dmc.archiving.storage.CloudStorageService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The form drops any prefill key that is not a field of the standard's intake
 * entity, so a generator that invents its own names silently does nothing
 * (that was Noark 5 until its keys were aligned). Pin every dedicated
 * generator's keys to the definition it serves.
 */
class IntakePrefillKeysMatchDefinitionsTest {

    private final SchemeDefinitionLoader schemes = new SchemeDefinitionLoader();
    private final CloudStorageService storage = mock(CloudStorageService.class);

    private final List<IntakeGenerator> dedicated = List.of(
            new Noark5IntakeGenerator(storage), new OaisIntakeGenerator(storage),
            new PremisIntakeGenerator(storage), new DublinCoreIntakeGenerator(storage),
            new MetsIntakeGenerator(storage), new EadIntakeGenerator(storage),
            new BagitIntakeGenerator(storage), new IsadgIntakeGenerator(storage),
            new ModsIntakeGenerator(storage), new EarkIntakeGenerator(storage));

    private static FileMetadataInput sampleMetadata() {
        FileMetadataInput m = new FileMetadataInput();
        m.setFilename("records-2026.pdf");
        m.setContentType("application/pdf");
        m.setFileSize(2048L);
        m.setChecksum("abc123");
        m.setUploadedAt("2026-01-15");
        m.setUploaderName("Alice");
        m.setFileCount(1);
        return m;
    }

    @Test
    void everyDedicatedGeneratorPrefillsOnlyFieldsItsIntakeEntityDeclares() {
        assertThat(dedicated).hasSize(ArchiveStandard.values().length);
        for (IntakeGenerator g : dedicated) {
            ArchiveStandard std = g.supportedStandard();
            assertThat(std).as(g.getClass().getSimpleName() + " is a dedicated generator").isNotNull();
            EntityDefinition intake = schemes.stageEntity(std, SchemeStage.INTAKE).orElseThrow();
            List<String> declared = intake.fields().stream().map(FieldDefinition::name).toList();

            Map<String, String> prefill = g.prefillFields(sampleMetadata());

            assertThat(prefill).as("%s prefill keys", std).isNotEmpty();
            assertThat(prefill.keySet()).as("%s prefill keys ⊆ %s fields", std, intake.name())
                    .isSubsetOf(declared);
        }
    }

    @Test
    void noark5PrefillPopulatesItsRequiredArchiveFields() {
        Map<String, String> prefill = new Noark5IntakeGenerator(storage).prefillFields(sampleMetadata());
        assertThat(prefill).containsEntry("title", "records-2026")
                .containsEntry("createdBy", "Alice")
                .containsEntry("createdDate", "2026-01-15")
                .containsKeys("systemID", "archiveStatus", "documentMedium", "closedDate", "closedBy");
    }
}
