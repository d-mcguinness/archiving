package com.dmc.archiving.scheme;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.scheme.api.SchemeDefinition;
import com.dmc.archiving.scheme.api.SchemeDefinitionApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * Public read access to the standard definitions. No auth guard on purpose:
 * the definitions are documentation of published standards and back the
 * public /ingest, /preserve and /deliver pages as well as the signed-in forms.
 */
@Controller
public class SchemeController {

    private final SchemeDefinitionApi schemes;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SchemeController(SchemeDefinitionApi schemes) {
        this.schemes = schemes;
    }

    @QueryMapping
    public SchemeDefinition standardDefinition(@Argument ArchiveStandard standard) {
        return schemes.get(standard).orElse(null);
    }

    @QueryMapping
    public List<SchemeDefinition> allStandardDefinitions() {
        return schemes.all();
    }

    /** Vocabularies have per-standard shapes, so they cross the wire as one JSON string. */
    @SchemaMapping(typeName = "SchemeDefinition", field = "vocabulariesJson")
    public String vocabulariesJson(SchemeDefinition def) {
        try {
            return objectMapper.writeValueAsString(def.vocabularies());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise vocabularies for " + def.standard(), e);
        }
    }

    /** {@code stageEntities} is a Map in the record; the schema types it as an object. */
    @SchemaMapping(typeName = "SchemeDefinition", field = "stageEntities")
    public Map<String, String> stageEntities(SchemeDefinition def) {
        return def.stageEntities();
    }
}
