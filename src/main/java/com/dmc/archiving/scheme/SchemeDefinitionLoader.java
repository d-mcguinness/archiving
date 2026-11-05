package com.dmc.archiving.scheme;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.scheme.api.EntityDefinition;
import com.dmc.archiving.scheme.api.FieldDefinition;
import com.dmc.archiving.scheme.api.FieldOption;
import com.dmc.archiving.scheme.api.SchemeDefinition;
import com.dmc.archiving.scheme.api.SchemeDefinitionApi;
import com.dmc.archiving.scheme.api.SchemeStage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads every {@code classpath:schemes/*.json} once at startup, validates the
 * parts the application relies on (a known {@code standardCode}, a root entity
 * for each stage, four keys on every field) and serves them immutably.
 *
 * A malformed definition fails application start rather than a user's page.
 */
@Service
public class SchemeDefinitionLoader implements SchemeDefinitionApi {

    private static final Logger log = LoggerFactory.getLogger(SchemeDefinitionLoader.class);
    static final String LOCATION_PATTERN = "classpath*:schemes/*.json";
    private static final Set<String> HEADER_KEYS = Set.of(
            "standard", "standardCode", "fullName", "reference", "description", "version",
            "xsdReference", "url", "stageEntities", "entities");
    private static final Set<String> FIELD_TYPES = Set.of("string", "text", "date", "number");

    private final Map<ArchiveStandard, SchemeDefinition> byStandard = new EnumMap<>(ArchiveStandard.class);

    public SchemeDefinitionLoader() {
        // Jackson 2 mapper built locally, like the package generators do: the Boot 4
        // context does not expose a Jackson 2 ObjectMapper bean.
        this(new ObjectMapper(), LOCATION_PATTERN);
    }

    SchemeDefinitionLoader(ObjectMapper objectMapper, String locationPattern) {
        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(locationPattern);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list scheme definitions at " + locationPattern, e);
        }
        for (Resource r : resources) {
            SchemeDefinition def = parse(objectMapper, r);
            SchemeDefinition previous = byStandard.putIfAbsent(def.standard(), def);
            if (previous != null) {
                throw new IllegalStateException("Two scheme definitions for " + def.standard()
                        + " (" + r.getFilename() + ")");
            }
        }
        if (byStandard.isEmpty()) {
            throw new IllegalStateException("No scheme definitions found at " + locationPattern);
        }
        log.info("Loaded {} scheme definitions: {}", byStandard.size(), byStandard.keySet());
    }

    private static SchemeDefinition parse(ObjectMapper mapper, Resource resource) {
        String file = resource.getFilename();
        JsonNode root;
        try {
            root = mapper.readTree(resource.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot parse scheme definition " + file, e);
        }

        String code = text(root, "standardCode");
        if (code == null) {
            throw new IllegalStateException(file + ": missing \"standardCode\" (an ArchiveStandard constant)");
        }
        ArchiveStandard standard = Arrays.stream(ArchiveStandard.values())
                .filter(s -> s.name().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalStateException(file + ": unknown standardCode \"" + code + "\""));

        List<EntityDefinition> entities = new ArrayList<>();
        for (JsonNode e : root.path("entities")) {
            List<FieldDefinition> fields = new ArrayList<>();
            for (JsonNode f : e.path("fields")) {
                String type = text(f, "type");
                if (text(f, "name") == null || text(f, "label") == null || type == null || !f.has("required")) {
                    throw new IllegalStateException(file + ": entity \"" + text(e, "name")
                            + "\" has a field without name/label/type/required: " + f);
                }
                if (!FIELD_TYPES.contains(type)) {
                    throw new IllegalStateException(file + ": field \"" + text(f, "name")
                            + "\" has unsupported type \"" + type + "\"");
                }
                String vocabulary = text(f, "vocabulary");
                List<FieldOption> options = vocabulary == null ? List.of()
                        : resolveVocabulary(root, vocabulary, file, text(e, "name") + "." + text(f, "name"));
                fields.add(new FieldDefinition(text(f, "name"), text(f, "label"), type,
                        f.get("required").asBoolean(), vocabulary, options));
            }
            entities.add(new EntityDefinition(
                    text(e, "name"), text(e, "type"), text(e, "description"), text(e, "parent"),
                    mapper.convertValue(e.path("children"), new TypeReference<List<String>>() {}),
                    text(e, "note"), fields));
        }

        Map<String, String> stageEntities = root.has("stageEntities")
                ? mapper.convertValue(root.get("stageEntities"), new TypeReference<Map<String, String>>() {})
                : Map.of();
        Set<String> entityNames = entities.stream().map(EntityDefinition::name).collect(java.util.stream.Collectors.toSet());
        for (SchemeStage stage : SchemeStage.values()) {
            String name = stageEntities.get(stage.key());
            if (name == null || !entityNames.contains(name)) {
                throw new IllegalStateException(file + ": stageEntities." + stage.key()
                        + " must name an existing entity (was " + name + ")");
            }
        }

        Map<String, Object> vocabularies = new LinkedHashMap<>();
        root.fields().forEachRemaining(entry -> {
            if (!HEADER_KEYS.contains(entry.getKey())) {
                vocabularies.put(entry.getKey(), mapper.convertValue(entry.getValue(), Object.class));
            }
        });

        return new SchemeDefinition(
                standard,
                text(root, "standard"),
                text(root, "fullName"),
                text(root, "reference"),
                text(root, "description"),
                text(root, "version"),
                text(root, "xsdReference"),
                text(root, "url"),
                stageEntities,
                entities,
                vocabularies);
    }

    /**
     * Resolve a field's {@code vocabulary} dot path against the definition. A
     * vocabulary is either a list of values, or an object whose keys are the
     * values and whose string values are their descriptions (used as labels).
     */
    private static List<FieldOption> resolveVocabulary(JsonNode root, String path, String file, String field) {
        JsonNode node = root;
        for (String segment : path.split("\\.")) {
            node = node.get(segment);
            if (node == null) {
                throw new IllegalStateException(file + ": field " + field + " references unknown vocabulary \"" + path + "\"");
            }
        }
        List<FieldOption> options = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode v : node) {
                if (!v.isTextual()) {
                    throw new IllegalStateException(file + ": vocabulary \"" + path + "\" must be a list of strings");
                }
                options.add(new FieldOption(v.asText(), null));
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                if (!entry.getValue().isTextual()) {
                    throw new IllegalStateException(file + ": vocabulary \"" + path
                            + "\" must map values to string descriptions (key \"" + entry.getKey() + "\")");
                }
                String label = entry.getValue().asText();
                options.add(new FieldOption(entry.getKey(), label.equals(entry.getKey()) ? null : label));
            });
        } else {
            throw new IllegalStateException(file + ": vocabulary \"" + path + "\" is neither a list nor an object");
        }
        if (options.isEmpty()) {
            throw new IllegalStateException(file + ": vocabulary \"" + path + "\" is empty");
        }
        return options;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    @Override
    public List<SchemeDefinition> all() {
        return List.copyOf(byStandard.values());
    }

    @Override
    public Optional<SchemeDefinition> get(ArchiveStandard standard) {
        return Optional.ofNullable(standard == null ? null : byStandard.get(standard));
    }
}
