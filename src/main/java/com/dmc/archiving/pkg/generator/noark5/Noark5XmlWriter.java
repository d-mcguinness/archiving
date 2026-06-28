package com.dmc.archiving.pkg.generator.noark5;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Emits Noark 5.5-shaped <code>arkivuttrekk</code> XML. The structure follows the
 * Noark 5 entity hierarchy (Arkiv → Arkivdel → optional Klassifikasjonssystem /
 * Mappe), but is intentionally built from the data we actually have on a Intake /
 * Preservation / Release — this is a well-formed Noark-shaped document, not a
 * schema-validated production arkivuttrekk (which would need the full Klasse /
 * Mappe / Registrering / Dokumentbeskrivelse hierarchy our model doesn't carry).
 */
public final class Noark5XmlWriter {

    public static final String NAMESPACE = "http://www.arkivverket.no/standarder/noark5/arkivstruktur";
    public static final String STANDARD_VERSION = "5.5";

    private static final XMLOutputFactory FACTORY = XMLOutputFactory.newInstance();

    private Noark5XmlWriter() {}

    /** Lifecycle marker — SIP / AIP / DIP. Controls statuses and date elements. */
    public enum Stage { SIP, AIP, DIP }

    public record ExtractInput(
            Stage stage,
            Long packageId,
            String title,
            String description,
            String createdAt,
            String updatedAt,
            String createdBy,
            String status,
            Element rootElement,
            Long sourcePackageId
    ) {}

    public static byte[] write(ExtractInput in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        try {
            XMLStreamWriter w = FACTORY.createXMLStreamWriter(out, "UTF-8");
            w.writeStartDocument("UTF-8", "1.0");
            writeNewline(w);
            w.setDefaultNamespace(NAMESPACE);
            w.writeStartElement(NAMESPACE, "arkivuttrekk");
            w.writeDefaultNamespace(NAMESPACE);
            w.writeAttribute("standardVersjon", STANDARD_VERSION);
            w.writeAttribute("uttrekkType", in.stage().name());
            writeNewline(w);

            writeMetadata(w, in);
            writeArkiv(w, in);

            w.writeEndElement(); // arkivuttrekk
            w.writeEndDocument();
            w.flush();
            w.close();
        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to write Noark 5 XML", e);
        }
        return out.toByteArray();
    }

    // ---------- sections ----------

    private static void writeMetadata(XMLStreamWriter w, ExtractInput in) throws XMLStreamException {
        w.writeStartElement(NAMESPACE, "uttrekkMetadata");
        textElement(w, "systemID", in.stage().name().toLowerCase() + "-" + in.packageId());
        textElement(w, "opprettetDato", in.createdAt());
        textElement(w, "opprettetAv", nullToEmpty(in.createdBy()));
        if (in.sourcePackageId() != null) {
            textElement(w, "kildeIdentifikator", String.valueOf(in.sourcePackageId()));
        }
        w.writeEndElement();
        writeNewline(w);
    }

    private static void writeArkiv(XMLStreamWriter w, ExtractInput in) throws XMLStreamException {
        w.writeStartElement(NAMESPACE, "arkiv");
        textElement(w, "systemID", "arkiv-" + in.packageId());
        textElement(w, "tittel", nullToEmpty(in.title()));
        textElement(w, "beskrivelse", nullToEmpty(in.description()));
        textElement(w, "arkivstatus", arkivstatusFor(in.stage(), in.status()));
        textElement(w, "dokumentmedium", "Elektronisk arkiv");
        textElement(w, "opprettetDato", nullToEmpty(in.createdAt()));
        if (in.updatedAt() != null) {
            textElement(w, "oppdatertDato", in.updatedAt());
        }
        if (in.stage() == Stage.AIP) {
            textElement(w, "avlevertDato", nullToEmpty(in.createdAt()));
        }

        writeArkivdel(w, in);

        w.writeEndElement(); // arkiv
        writeNewline(w);
    }

    private static void writeArkivdel(XMLStreamWriter w, ExtractInput in) throws XMLStreamException {
        Element root = in.rootElement();
        w.writeStartElement(NAMESPACE, "arkivdel");

        String id = root != null && root.getElementIdentifier() != null
                ? root.getElementIdentifier()
                : "arkivdel-" + in.packageId();
        textElement(w, "systemID", id);
        textElement(w, "tittel", root != null && root.getTitle() != null ? root.getTitle() : nullToEmpty(in.title()));
        textElement(w, "beskrivelse", root != null && root.getDescription() != null ? root.getDescription() : nullToEmpty(in.description()));
        textElement(w, "arkivdelstatus", arkivstatusFor(in.stage(), in.status()));
        textElement(w, "dokumentmedium", "Elektronisk arkiv");
        if (root != null && root.getCreatedAt() != null) {
            textElement(w, "opprettetDato", root.getCreatedAt().toString());
        }
        if (root != null && root.getCreatedBy() != null) {
            textElement(w, "opprettetAv", root.getCreatedBy());
        }

        if (root != null) {
            writeKlassifikasjon(w, root);
        }

        w.writeEndElement(); // arkivdel
    }

    private static void writeKlassifikasjon(XMLStreamWriter w, Element root) throws XMLStreamException {
        w.writeStartElement(NAMESPACE, "klassifikasjonssystem");
        textElement(w, "systemID", "klass-" + (root.getElementIdentifier() != null ? root.getElementIdentifier() : "default"));
        textElement(w, "tittel", root.getNorwegianName() != null ? root.getNorwegianName() : nullToEmpty(root.getEntityName()));
        if (root.getEnglishName() != null) {
            textElement(w, "beskrivelse", root.getEnglishName());
        }

        List<Field> fields = root.getFields();
        if (fields != null && !fields.isEmpty()) {
            for (Field f : fields) {
                writeFieldAsKlasse(w, f);
            }
        }

        w.writeEndElement(); // klassifikasjonssystem
    }

    private static void writeFieldAsKlasse(XMLStreamWriter w, Field f) throws XMLStreamException {
        w.writeStartElement(NAMESPACE, "klasse");
        textElement(w, "klasseID", nullToEmpty(f.getName()));
        textElement(w, "tittel", nullToEmpty(f.getName()));
        textElement(w, "beskrivelse", nullToEmpty(f.getValue()));
        w.writeEndElement();
    }

    // ---------- helpers ----------

    private static String arkivstatusFor(Stage stage, String packageStatus) {
        return switch (stage) {
            case SIP -> "Opprettet";
            case AIP -> "Bevart";
            case DIP -> "Avlevert";
        };
    }

    private static void textElement(XMLStreamWriter w, String name, String value) throws XMLStreamException {
        w.writeStartElement(NAMESPACE, name);
        if (value != null) w.writeCharacters(value);
        w.writeEndElement();
    }

    private static void writeNewline(XMLStreamWriter w) throws XMLStreamException {
        w.writeCharacters("\n");
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static String filename(Stage stage) {
        return "noark5_" + stage.name().toLowerCase() + ".xml";
    }

    public static byte[] formatPretty(byte[] xmlBytes) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            ByteArrayOutputStream out = new ByteArrayOutputStream(xmlBytes.length + 256);
            t.transform(new StreamSource(new ByteArrayInputStream(xmlBytes)), new StreamResult(out));
            return out.toByteArray();
        } catch (Exception e) {
            // If indentation fails for any reason, return the unformatted bytes —
            // they're still valid XML.
            return xmlBytes;
        }
    }
}
