package com.dmc.archiving.export;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class XmlDocumentWriterTest {

    @Test
    void mirrorsMapsListsAndScalarsAndEscapes() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("title", "Tom & \"Jerry\" <2026>");
        doc.put("count", 3);
        doc.put("flag", true);
        doc.put("nothing", null);
        doc.put("nested", Map.of("inner", "v"));
        doc.put("list", List.of("a", Map.of("k", 1)));
        doc.put("array", new Object[]{"x", null, "y"});

        String xml = new String(XmlDocumentWriter.write("root", doc), StandardCharsets.UTF_8);

        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("<root>").endsWith("</root>\n");
        assertThat(xml).contains("<title>Tom &amp; \"Jerry\" &lt;2026&gt;</title>");
        assertThat(xml).contains("<count>3</count>").contains("<flag>true</flag>");
        assertThat(xml).doesNotContain("<nothing");
        assertThat(xml).contains("<nested>").contains("<inner>v</inner>");
        assertThat(xml).contains("<list>").contains("<item>a</item>").contains("<item>").contains("<k>1</k>");
        assertThat(xml).contains("<array>").contains("<item>x</item>").contains("<item>y</item>");
        // Each list entry is its own <item>: two for the list, two for the array (null skipped)
        assertThat(xml.split("<item>", -1).length - 1).isEqualTo(4);
    }

    @Test
    void makesKeysValidElementNamesAndKeepsTheOriginal() {
        assertThat(XmlDocumentWriter.elementName("createdAt")).isEqualTo("createdAt");
        assertThat(XmlDocumentWriter.elementName("Physical medium")).isEqualTo("Physical_medium");
        assertThat(XmlDocumentWriter.elementName("2026-plan")).isEqualTo("_2026-plan");
        assertThat(XmlDocumentWriter.elementName("")).isEqualTo("_");

        String xml = new String(XmlDocumentWriter.write("r", Map.of("Physical medium", "yes")), StandardCharsets.UTF_8);
        assertThat(xml).contains("<Physical_medium name=\"Physical medium\">yes</Physical_medium>");
    }

    @Test
    void isWellFormedForADeepDocument() throws Exception {
        Map<String, Object> doc = Map.of("a", Map.of("b", Map.of("c", List.of(Map.of("d", "e")))));
        byte[] xml = XmlDocumentWriter.write("deep", doc);
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        var parsed = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml));
        assertThat(parsed.getDocumentElement().getNodeName()).isEqualTo("deep");
        assertThat(parsed.getElementsByTagName("d").item(0).getTextContent()).isEqualTo("e");
        assertThat(Arrays.asList("a", "b", "c", "item")).allSatisfy(tag ->
                assertThat(parsed.getElementsByTagName(tag).getLength()).isEqualTo(1));
    }
}
