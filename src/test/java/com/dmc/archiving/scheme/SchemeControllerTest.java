package com.dmc.archiving.scheme;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.scheme.api.SchemeDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SchemeControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SchemeController controller = new SchemeController(new SchemeDefinitionLoader());

    @Test
    void returnsTheDefinitionOrNull() {
        assertThat(controller.standardDefinition(ArchiveStandard.EARK)).isNotNull()
                .extracting(SchemeDefinition::standard).isEqualTo(ArchiveStandard.EARK);
        assertThat(controller.standardDefinition(null)).isNull();
        assertThat(controller.allStandardDefinitions()).hasSize(ArchiveStandard.values().length);
    }

    @Test
    void vocabulariesCrossTheWireAsJson() throws Exception {
        SchemeDefinition n5 = controller.standardDefinition(ArchiveStandard.NOARK5);
        String json = controller.vocabulariesJson(n5);
        Map<String, Object> parsed = mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        assertThat(parsed).containsKey("statusValues");
        assertThat(((Map<?, ?>) parsed.get("statusValues")).get("archiveStatus"))
                .isEqualTo(List.of("Created", "Closed", "Transferred", "Periodized", "Deleted"));
        assertThat(controller.stageEntities(n5)).containsEntry("intake", "Archive");
    }
}
