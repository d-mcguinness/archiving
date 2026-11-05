package com.dmc.archiving.scheme;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The definitions are reachable through the real GraphQL schema, unauthenticated
 * (they back the public standards pages), with the nested entity/field types and
 * the stageEntities + vocabulariesJson mappings wired.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SchemeGraphQlEndpointTest {

    @Autowired private MockMvc mvc;

    private static String body(String query) {
        return "{\"query\":\"" + query.replace("\"", "\\\"") + "\"}";
    }

    @Test
    void standardDefinitionIsServedUnauthenticated() throws Exception {
        String q = "{ standardDefinition(standard: NOARK5) { standard displayName fullName "
                + "stageEntities { intake preservation release } "
                + "entities { name children fields { name label type required vocabulary options { value label } } } vocabulariesJson } }";

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body(q)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.standardDefinition.standard").value("NOARK5"))
                .andExpect(jsonPath("$.data.standardDefinition.displayName").value("NOARK5"))
                .andExpect(jsonPath("$.data.standardDefinition.fullName").value("Norsk Arkivstandard 5"))
                .andExpect(jsonPath("$.data.standardDefinition.stageEntities.intake").value("Archive"))
                .andExpect(jsonPath("$.data.standardDefinition.entities[0].name").value("Archive"))
                .andExpect(jsonPath("$.data.standardDefinition.entities[0].children", hasItem("Series")))
                .andExpect(jsonPath("$.data.standardDefinition.entities[0].fields[*].name", hasItem("createdDate")))
                .andExpect(jsonPath("$.data.standardDefinition.entities[0].fields[0].required").value(true))
                .andExpect(jsonPath("$.data.standardDefinition.entities[0].fields[?(@.name=='archiveStatus')].vocabulary")
                        .value(hasItem("statusValues.archiveStatus")))
                .andExpect(jsonPath("$.data.standardDefinition.entities[0].fields[?(@.name=='archiveStatus')].options[*].value")
                        .value(hasItem("Created")))
                .andExpect(jsonPath("$.data.standardDefinition.entities[0].fields[?(@.name=='title')].options", hasItem(hasSize(0))))
                .andExpect(jsonPath("$.data.standardDefinition.vocabulariesJson", containsString("statusValues")));
    }

    @Test
    void allStandardDefinitionsCoverEveryStandard() throws Exception {
        String q = "{ allStandardDefinitions { standard stageEntities { release } } }";

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body(q)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.allStandardDefinitions", hasSize(10)))
                .andExpect(jsonPath("$.data.allStandardDefinitions[*].standard", hasItem("DUBLIN_CORE")))
                .andExpect(jsonPath("$.data.allStandardDefinitions[*].stageEntities.release",
                        hasItem("Dissemination Information Package")));
    }
}
