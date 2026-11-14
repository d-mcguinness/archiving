package com.dmc.archiving.export;

import com.dmc.archiving.archive.api.ArchiveExportApi;
import com.dmc.archiving.archive.api.ArchiveExportInfo;
import com.dmc.archiving.scheme.SchemeDefinitionLoader;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The archive extract lays the archive's elements out by its standard's real
 * definition (classpath:schemes/*.json) and reports where they fall short of it.
 */
class ArchiveExtractServiceTest {

    private final ArchiveExportApi archives = mock(ArchiveExportApi.class);
    private final ArchiveExtractService service = new ArchiveExtractService(archives, new SchemeDefinitionLoader());

    @SuppressWarnings("unchecked")
    private static Map<String, Object> m(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> l(Object o) {
        return (List<Map<String, Object>>) o;
    }

    /** A stored element in the shape AbstractArchiveStrategy.exportElement emits. */
    @SafeVarargs
    private static Map<String, Object> stored(String entity, String identifier, Map<String, String> fields,
                                              Map<String, Object>... children) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("id", 1L);
        e.put("elementIdentifier", identifier);
        e.put("entityName", entity);
        e.put("entityType", "stored_type");
        e.put("title", identifier + " title");
        Map<String, Object> fs = new LinkedHashMap<>();
        fields.forEach((k, v) -> fs.put(k, Map.of("label", k, "type", "string", "value", v)));
        if (!fs.isEmpty()) e.put("fields", fs);
        if (children.length > 0) e.put("children", List.of(children));
        return e;
    }

    private static ArchiveExportInfo premis(Object... topLevel) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", 3L);
        data.put("title", "Digital Asset Library");
        data.put("elements", List.of(topLevel));
        data.put("standardName", "PREMIS");
        return new ArchiveExportInfo(3L, 2L, "Digital Asset Library", "PREMIS", "PREMIS", data);
    }

    private static Map<String, String> ordered(String... kv) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put(kv[i], kv[i + 1]);
        return map;
    }

    @Test
    void keepsTheStoredDocumentAndAddsTheStandardStructure() {
        Map<String, Object> doc = service.compose(premis(stored("Agent", "A1",
                ordered("agentIdentifierValue", "Bob", "agentIdentifierType", "local"))));

        // Existing keys are untouched.
        assertThat(doc.get("title")).isEqualTo("Digital Asset Library");
        assertThat(l(doc.get("elements"))).hasSize(1);

        Map<String, Object> structure = m(doc.get("standardStructure"));
        Map<String, Object> standard = m(structure.get("standard"));
        assertThat(standard.get("standard")).isEqualTo("PREMIS");
        assertThat(standard.get("name")).isNotNull();
        assertThat(standard.get("reference")).isNotNull();

        Map<String, Object> agent = l(structure.get("elements")).get(0);
        assertThat(agent.get("entity")).isEqualTo("Agent");
        assertThat(agent.get("entityType")).isEqualTo("core_entity"); // from the definition, not the stored value
        assertThat(agent.get("identifier")).isEqualTo("A1");
        // Fields come out in the standard's order, whatever order they were stored in.
        assertThat(m(agent.get("fields")).keySet()).containsExactly("agentIdentifierType", "agentIdentifierValue");

        assertThat(m(structure.get("summary")).get("elements")).isEqualTo(1);
        assertThat(m(m(structure.get("summary")).get("byEntity"))).containsEntry("Agent", 1);
        Map<String, Object> conformance = m(structure.get("conformance"));
        assertThat(conformance.get("checked")).isEqualTo(true);
        assertThat(conformance.get("conformant")).isEqualTo(true);
        assertThat(l(conformance.get("issues"))).isEmpty();
    }

    @Test
    void reportsWhereElementsFallShortOfTheStandard() {
        Map<String, Object> doc = service.compose(premis(
                // File belongs under Representation; a required field is missing; one field is not PREMIS's.
                stored("File", "F1", ordered("objectIdentifierType", "local", "objectIdentifierValue", "X1",
                        "messageDigest", "abc", "formatName", "text/plain", "shoeSize", "44")),
                stored("Series", "S1", ordered("title", "Not a PREMIS entity"))));

        Map<String, Object> structure = m(doc.get("standardStructure"));
        List<Map<String, Object>> issues = l(m(structure.get("conformance")).get("issues"));
        assertThat(m(structure.get("conformance")).get("conformant")).isEqualTo(false);
        assertThat(issues).extracting(i -> i.get("element") + ": " + i.get("issue"))
                .anySatisfy(s -> assertThat(s).startsWith("F1: belongs under \"Representation\"").contains("top level"))
                .anySatisfy(s -> assertThat(s).startsWith("F1: required field").contains("Message Digest Algorithm"))
                .anySatisfy(s -> assertThat(s).startsWith("F1: carries fields the standard does not define").contains("shoeSize"))
                .anySatisfy(s -> assertThat(s).startsWith("S1: \"Series\" is not an entity of PREMIS"));

        Map<String, Object> file = l(structure.get("elements")).get(0);
        assertThat(m(file.get("fields"))).doesNotContainKey("shoeSize");
        assertThat(m(file.get("otherFields"))).containsEntry("shoeSize", "44");
        // An unknown entity keeps its fields as stored.
        assertThat(m(l(structure.get("elements")).get(1).get("fields"))).containsEntry("title", "Not a PREMIS entity");
    }

    @Test
    void flagsAValueOutsideAFieldsVocabulary() {
        Map<String, Object> doc = service.compose(premis(stored("Representation", "R1",
                ordered("objectIdentifierType", "local", "objectIdentifierValue", "R1"),
                stored("File", "F1", ordered("objectIdentifierType", "local", "objectIdentifierValue", "X1",
                        "messageDigestAlgorithm", "ROT13", "messageDigest", "abc", "formatName", "text/plain")))));

        Map<String, Object> structure = m(doc.get("standardStructure"));
        List<Map<String, Object>> issues = l(m(structure.get("conformance")).get("issues"));
        assertThat(issues).extracting(i -> String.valueOf(i.get("issue")))
                .anySatisfy(s -> assertThat(s).contains("ROT13").contains("not an allowed value"));
        // Nested correctly, so no placement issue for the File; children are carried through.
        assertThat(issues).filteredOn(i -> "F1".equals(i.get("element")))
                .extracting(i -> String.valueOf(i.get("issue"))).noneMatch(s -> s.startsWith("belongs under"));
        Map<String, Object> representation = l(structure.get("elements")).get(0);
        assertThat(l(representation.get("children")).get(0).get("entity")).isEqualTo("File");
        assertThat(m(structure.get("summary")).get("elements")).isEqualTo(2);
    }

    @Test
    void usesTheRootElementWhenTheArchiveHasOne() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rootElement", stored("Agent", "ROOT", ordered("agentIdentifierType", "local", "agentIdentifierValue", "x")));
        ArchiveExportInfo info = new ArchiveExportInfo(3L, 2L, "t", "PREMIS", "PREMIS", data);

        List<Map<String, Object>> elements = l(m(service.compose(info).get("standardStructure")).get("elements"));

        assertThat(elements).hasSize(1);
        assertThat(elements.get(0).get("identifier")).isEqualTo("ROOT");
    }

    @Test
    void anEmptyArchiveIsConformantAndHasNoElements() {
        ArchiveExportInfo info = new ArchiveExportInfo(3L, 2L, "t", "PREMIS", "PREMIS", new LinkedHashMap<>());

        Map<String, Object> structure = m(service.compose(info).get("standardStructure"));

        assertThat(l(structure.get("elements"))).isEmpty();
        assertThat(m(structure.get("summary")).get("elements")).isEqualTo(0);
        assertThat(m(structure.get("conformance")).get("conformant")).isEqualTo(true);
    }

    @Test
    void rendersJsonAndXmlWithAStandardNamedFile() {
        when(archives.loadForExport(3L)).thenReturn(premis(stored("Agent", "A1",
                ordered("agentIdentifierType", "local", "agentIdentifierValue", "Bob"))));

        ArchiveExtractService.Rendered json = service.render(3L, PreservationExtractService.Format.JSON);
        ArchiveExtractService.Rendered xml = service.render(3L, PreservationExtractService.Format.XML);

        assertThat(json.filename()).isEqualTo("archive_3_PREMIS_export.json");
        assertThat(json.contentType()).isEqualTo("application/json");
        assertThat(json.tenantId()).isEqualTo(2L);
        assertThat(new String(json.bytes(), StandardCharsets.UTF_8)).contains("\"standardStructure\"");
        assertThat(xml.filename()).isEqualTo("archive_3_PREMIS_export.xml");
        assertThat(new String(xml.bytes(), StandardCharsets.UTF_8))
                .contains("<archiveExtract>").contains("<standardStructure>").contains("<agentIdentifierValue>Bob</agentIdentifierValue>");
    }

    @Test
    void aMissingArchiveRendersNothing() {
        when(archives.loadForExport(9L)).thenReturn(null);
        assertThat(service.render(9L, PreservationExtractService.Format.JSON)).isNull();
    }
}
