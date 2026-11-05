package com.dmc.archiving.export;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

/**
 * Renders a JSON-shaped document (maps, lists, scalars) as XML with the same
 * structure: a map becomes child elements named by its keys, a list becomes
 * repeated {@code <item>} elements, scalars become text, nulls are omitted.
 * Keys are made into valid element names (invalid characters → "_", leading
 * digit prefixed), with the original key kept in a {@code name} attribute
 * whenever it had to change.
 */
public final class XmlDocumentWriter {

    private static final XMLOutputFactory FACTORY = XMLOutputFactory.newInstance();

    private XmlDocumentWriter() {}

    public static byte[] write(String rootName, Map<String, ?> document) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        try {
            XMLStreamWriter w = FACTORY.createXMLStreamWriter(out, StandardCharsets.UTF_8.name());
            w.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            w.writeCharacters("\n");
            writeElement(w, rootName, document, 0);
            w.writeEndDocument();
            w.flush();
            w.close();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Failed to write XML document", e);
        }
        return out.toByteArray();
    }

    private static void writeElement(XMLStreamWriter w, String key, Object value, int depth) throws XMLStreamException {
        if (value == null) return;
        String name = elementName(key);
        indent(w, depth);
        w.writeStartElement(name);
        if (!name.equals(key)) {
            w.writeAttribute("name", key);
        }
        if (value instanceof Map<?, ?> map) {
            w.writeCharacters("\n");
            for (Map.Entry<?, ?> e : map.entrySet()) {
                writeElement(w, String.valueOf(e.getKey()), e.getValue(), depth + 1);
            }
            indent(w, depth);
        } else if (value instanceof Collection<?> list) {
            w.writeCharacters("\n");
            for (Object item : list) {
                if (item == null) continue;
                writeElement(w, "item", item, depth + 1);
            }
            indent(w, depth);
        } else if (value.getClass().isArray()) {
            w.writeCharacters("\n");
            for (Object item : (Object[]) value) {
                if (item == null) continue;
                writeElement(w, "item", item, depth + 1);
            }
            indent(w, depth);
        } else {
            w.writeCharacters(String.valueOf(value));
        }
        w.writeEndElement();
        w.writeCharacters("\n");
    }

    private static void indent(XMLStreamWriter w, int depth) throws XMLStreamException {
        for (int i = 0; i < depth; i++) w.writeCharacters("  ");
    }

    /** A valid XML NCName derived from {@code key}. */
    static String elementName(String key) {
        if (key == null || key.isEmpty()) return "_";
        StringBuilder sb = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            boolean ok = Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.';
            if (i == 0 && !(Character.isLetter(c) || c == '_')) {
                sb.append('_');
                if (ok) sb.append(c);
            } else {
                sb.append(ok ? c : '_');
            }
        }
        return sb.toString();
    }
}
