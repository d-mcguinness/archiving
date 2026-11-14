package com.dmc.archiving.export;

import com.dmc.archiving.archive.api.ArchiveExportApi;
import com.dmc.archiving.archive.api.ArchiveExportInfo;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.scheme.api.EntityDefinition;
import com.dmc.archiving.scheme.api.FieldDefinition;
import com.dmc.archiving.scheme.api.SchemeDefinition;
import com.dmc.archiving.scheme.api.SchemeDefinitionApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Composes the extract document for one archive.
 *
 * <p>The document keeps the archive's own description and its element tree as
 * stored ({@code rootElement} / {@code elements}), and adds
 * {@code standardStructure}: the same elements laid out the way the archive's
 * standard defines them —
 *
 * <pre>
 * standard     – which standard, its full name, reference and version
 * elements     – the tree, each element named by the standard's entity, its
 *                fields under the standard's field names in the standard's order
 * summary      – how many elements of each entity
 * conformance  – what the standard asks for that the archive does not meet:
 *                unknown entities, elements under the wrong parent, missing
 *                required fields, values outside a field's vocabulary
 * </pre>
 *
 * This is the standard's information model, not a schema-valid serialisation of
 * it (no METS/EAD/PREMIS XML schemas are targeted). The same document is
 * rendered as JSON or, via {@link XmlDocumentWriter}, XML.
 */
@Service
public class ArchiveExtractService {

    /** A rendered extract ready to be sent as a download. */
    public record Rendered(byte[] bytes, String filename, String contentType, Long tenantId) {}

    private final ArchiveExportApi archives;
    private final SchemeDefinitionApi schemes;
    private final ObjectMapper mapper;

    public ArchiveExtractService(ArchiveExportApi archives, SchemeDefinitionApi schemes) {
        this.archives = archives;
        this.schemes = schemes;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** Compose and serialise. Null when the archive does not exist. */
    public Rendered render(Long archiveId, PreservationExtractService.Format format) {
        ArchiveExportInfo info = archives.loadForExport(archiveId);
        if (info == null) {
            return null;
        }
        Map<String, Object> doc = compose(info);
        byte[] bytes = switch (format) {
            case JSON -> toJson(doc);
            case XML -> XmlDocumentWriter.write("archiveExtract", doc);
        };
        String filename = "archive_" + info.archiveId() + "_" + info.standard() + "_export." + format.extension;
        return new Rendered(bytes, filename, format.contentType, info.tenantId());
    }

    Map<String, Object> compose(ArchiveExportInfo info) {
        Map<String, Object> doc = new LinkedHashMap<>(info.data());
        doc.put("standardStructure", standardStructure(info));
        return doc;
    }

    private Map<String, Object> standardStructure(ArchiveExportInfo info) {
        SchemeDefinition scheme = scheme(info.standard());

        Map<String, Object> standard = new LinkedHashMap<>();
        standard.put("standard", info.standard());
        if (scheme != null) {
            standard.put("name", scheme.fullName());
            standard.put("reference", scheme.reference());
            standard.put("version", scheme.version());
            if (scheme.url() != null) standard.put("url", scheme.url());
        }

        List<Map<String, Object>> issues = new ArrayList<>();
        Map<String, Integer> byEntity = new TreeMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();
        for (Map<String, Object> stored : storedTopLevel(info.data())) {
            elements.add(element(stored, null, scheme, issues, byEntity));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("elements", byEntity.values().stream().mapToInt(Integer::intValue).sum());
        summary.put("byEntity", byEntity);

        Map<String, Object> conformance = new LinkedHashMap<>();
        if (scheme == null) {
            conformance.put("checked", false);
            conformance.put("reason", "No definition is loaded for " + info.standard());
        } else {
            conformance.put("checked", true);
            conformance.put("conformant", issues.isEmpty());
            conformance.put("issues", issues);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("standard", standard);
        out.put("elements", elements);
        out.put("summary", summary);
        out.put("conformance", conformance);
        return out;
    }

    /** One stored element as the standard describes it, recursing into its children. */
    private Map<String, Object> element(Map<String, Object> stored, String parentEntity, SchemeDefinition scheme,
                                        List<Map<String, Object>> issues, Map<String, Integer> byEntity) {
        String entityName = str(stored.get("entityName"));
        String identifier = str(stored.get("elementIdentifier"));
        byEntity.merge(entityName == null ? "(unnamed)" : entityName, 1, Integer::sum);

        EntityDefinition entity = scheme == null || entityName == null ? null : scheme.entity(entityName).orElse(null);
        Map<String, String> values = storedFieldValues(stored);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("entity", entityName);
        out.put("entityType", entity != null ? entity.type() : stored.get("entityType"));
        out.put("identifier", identifier);
        out.put("title", stored.get("title"));

        Map<String, Object> fields = new LinkedHashMap<>();
        if (entity == null) {
            fields.putAll(values);
            if (scheme != null) {
                issue(issues, identifier, entityName, "\"" + entityName + "\" is not an entity of " + scheme.displayName());
            }
        } else {
            if (entity.parent() != null && !entity.parent().equals(parentEntity)) {
                issue(issues, identifier, entityName, "belongs under \"" + entity.parent() + "\" but is "
                        + (parentEntity == null ? "at the top level" : "under \"" + parentEntity + "\""));
            }
            // The standard's fields first, in the standard's order.
            for (FieldDefinition field : entity.fields()) {
                String value = values.remove(field.name());
                if (value == null || value.isBlank()) {
                    if (field.required()) {
                        issue(issues, identifier, entityName, "required field \"" + field.label() + "\" is empty");
                    }
                    continue;
                }
                fields.put(field.name(), value);
                if (field.isEnumerated() && field.options().stream().noneMatch(o -> o.value().equals(value))) {
                    issue(issues, identifier, entityName,
                            "\"" + value + "\" is not an allowed value for \"" + field.label() + "\"");
                }
            }
            // Anything else the element carries is kept, apart from the standard's fields.
            if (!values.isEmpty()) {
                out.put("otherFields", new LinkedHashMap<>(values));
                issue(issues, identifier, entityName,
                        "carries fields the standard does not define: " + String.join(", ", values.keySet()));
            }
        }
        out.put("fields", fields);

        List<Map<String, Object>> children = new ArrayList<>();
        for (Map<String, Object> child : maps(stored.get("children"))) {
            children.add(element(child, entityName, scheme, issues, byEntity));
        }
        if (!children.isEmpty()) {
            out.put("children", children);
        }
        return out;
    }

    private static void issue(List<Map<String, Object>> issues, String identifier, String entity, String problem) {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("element", identifier);
        issue.put("entity", entity);
        issue.put("issue", problem);
        issues.add(issue);
    }

    /** The archive's tree as the archive module exported it: a single root, or its top-level elements. */
    private static List<Map<String, Object>> storedTopLevel(Map<String, Object> data) {
        List<Map<String, Object>> root = maps(data.get("rootElement"));
        return root.isEmpty() ? maps(data.get("elements")) : root;
    }

    /** Field name → value from a stored element's {@code fields} ({name: {label, type, value}}). */
    private static Map<String, String> storedFieldValues(Map<String, Object> stored) {
        Map<String, String> values = new LinkedHashMap<>();
        if (stored.get("fields") instanceof Map<?, ?> fields) {
            for (Map.Entry<?, ?> e : fields.entrySet()) {
                Object v = e.getValue() instanceof Map<?, ?> field ? field.get("value") : e.getValue();
                if (v != null) values.put(String.valueOf(e.getKey()), String.valueOf(v));
            }
        }
        return values;
    }

    /** A map, or a list of maps, as a list of maps; anything else as empty. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (value instanceof Map<?, ?> map) {
            out.add((Map<String, Object>) map);
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) out.add((Map<String, Object>) map);
            }
        }
        return out;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private SchemeDefinition scheme(String standardName) {
        try {
            return schemes.get(ArchiveStandard.valueOf(standardName)).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private byte[] toJson(Map<String, Object> doc) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(doc);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
