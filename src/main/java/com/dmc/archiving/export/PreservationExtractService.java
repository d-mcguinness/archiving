package com.dmc.archiving.export;

import com.dmc.archiving.archive.api.ArchiveExportApi;
import com.dmc.archiving.archive.api.ArchiveExportInfo;
import com.dmc.archiving.intake.api.IntakeExportApi;
import com.dmc.archiving.pkg.generator.PackagePayload;
import com.dmc.archiving.preservation.api.PreservationExportApi;
import com.dmc.archiving.preservation.api.PreservationExtract;
import com.dmc.archiving.scheme.api.SchemeDefinition;
import com.dmc.archiving.scheme.api.SchemeDefinitionApi;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Composes the extract document for one preservation:
 *
 * <pre>
 * extract        – what this is, when it was taken, the standard and its reference
 * preservation   – the package's own description (envelope + root element tree)
 * package        – the standard-conformant artifact its generator renders
 *                  (the E-ARK/OAIS/… JSON as a nested object, or an XML artifact
 *                  such as the Noark 5 arkivuttrekk as text under `content`)
 * sourceIntake   – the intake it was preserved from, when linked
 * archive        – that intake's archive, exported by the standard's strategy
 * </pre>
 *
 * The same document is rendered as JSON or, via {@link XmlDocumentWriter}, XML.
 */
@Service
public class PreservationExtractService {

    public enum Format {
        JSON("json", "application/json"),
        XML("xml", "application/xml");

        public final String extension;
        public final String contentType;

        Format(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        public static Format parse(String value) {
            if (value == null || value.isBlank() || "json".equalsIgnoreCase(value)) return JSON;
            if ("xml".equalsIgnoreCase(value)) return XML;
            throw new IllegalArgumentException("Unsupported format '" + value + "' (use json or xml)");
        }
    }

    /** A rendered extract ready to be sent as a download. */
    public record Rendered(byte[] bytes, String filename, String contentType, Long tenantId) {}

    private final PreservationExportApi preservations;
    private final IntakeExportApi intakes;
    private final ArchiveExportApi archives;
    private final SchemeDefinitionApi schemes;
    private final ObjectMapper mapper;

    public PreservationExtractService(PreservationExportApi preservations,
                                      IntakeExportApi intakes,
                                      ArchiveExportApi archives,
                                      SchemeDefinitionApi schemes) {
        this.preservations = preservations;
        this.intakes = intakes;
        this.archives = archives;
        this.schemes = schemes;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** The composed document, or empty when the preservation does not exist. */
    public Optional<Map<String, Object>> compose(Long preservationId) {
        return preservations.extract(preservationId).map(this::compose);
    }

    Map<String, Object> compose(PreservationExtract aip) {
        Map<String, Object> doc = new LinkedHashMap<>();

        Map<String, Object> extract = new LinkedHashMap<>();
        extract.put("type", "PRESERVATION");
        extract.put("preservationId", aip.id());
        extract.put("extractedAt", LocalDateTime.now().toString());
        extract.put("standard", aip.standard().name());
        schemes.get(aip.standard()).ifPresent(def -> {
            extract.put("standardName", def.fullName());
            extract.put("standardReference", def.reference());
            extract.put("standardVersion", def.version());
            if (def.url() != null) extract.put("standardUrl", def.url());
        });
        doc.put("extract", extract);

        doc.put("preservation", aip.data());
        doc.put("package", packageSection(aip.artifact()));

        Optional<Long> archiveId = intakes.archiveIdOf(aip.sourceIntakeId());
        if (aip.sourceIntakeId() != null) {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("intakeId", aip.sourceIntakeId());
            archiveId.ifPresent(id -> source.put("archiveId", id));
            doc.put("sourceIntake", source);
        }

        ArchiveExportInfo archive = archiveId.map(archives::loadForExport).orElse(null);
        if (archive != null) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", archive.archiveId());
            a.put("standard", archive.standard());
            a.put("standardReference", archive.standardReference());
            a.put("details", archive.data());
            doc.put("archive", a);
        } else {
            doc.put("archive", null);
        }
        return doc;
    }

    /** JSON artifacts are embedded as structure; anything else (Noark 5 XML, …) as text. */
    private Map<String, Object> packageSection(PackagePayload artifact) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("filename", artifact.filename());
        pkg.put("contentType", artifact.contentType());
        if (artifact.contentType() != null && artifact.contentType().contains("json")) {
            try {
                pkg.put("content", mapper.readValue(artifact.bytes(), new TypeReference<Map<String, Object>>() {}));
                return pkg;
            } catch (IOException e) {
                // fall through to text
            }
        }
        pkg.put("content", new String(artifact.bytes(), StandardCharsets.UTF_8));
        return pkg;
    }

    /** Compose and serialise in one step. Empty when the preservation does not exist. */
    public Optional<Rendered> render(Long preservationId, Format format) {
        return preservations.extract(preservationId).map(aip -> {
            Map<String, Object> doc = compose(aip);
            String base = "preservation_" + aip.id() + "_" + aip.standard().name() + "_extract." + format.extension;
            byte[] bytes = switch (format) {
                case JSON -> toJson(doc);
                case XML -> XmlDocumentWriter.write("preservationExtract", doc);
            };
            return new Rendered(bytes, base, format.contentType, aip.tenantId());
        });
    }

    private byte[] toJson(Map<String, Object> doc) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(doc);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
