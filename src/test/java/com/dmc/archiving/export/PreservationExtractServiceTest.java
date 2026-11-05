package com.dmc.archiving.export;

import com.dmc.archiving.archive.api.ArchiveExportApi;
import com.dmc.archiving.archive.api.ArchiveExportInfo;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.intake.api.IntakeExportApi;
import com.dmc.archiving.pkg.generator.PackagePayload;
import com.dmc.archiving.preservation.api.PreservationExportApi;
import com.dmc.archiving.preservation.api.PreservationExtract;
import com.dmc.archiving.scheme.SchemeDefinitionLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreservationExtractServiceTest {

    private final PreservationExportApi preservations = mock(PreservationExportApi.class);
    private final IntakeExportApi intakes = mock(IntakeExportApi.class);
    private final ArchiveExportApi archives = mock(ArchiveExportApi.class);
    private final PreservationExtractService service =
            new PreservationExtractService(preservations, intakes, archives, new SchemeDefinitionLoader());

    @SuppressWarnings("unchecked")
    private static Map<String, Object> m(Object o) {
        return (Map<String, Object>) o;
    }

    private static PreservationExtract eark(Long sourceIntakeId) {
        byte[] json = "{\"standard\":\"EARK\",\"metsHeader\":{\"oaisPackageType\":\"AIP\"}}".getBytes(StandardCharsets.UTF_8);
        return new PreservationExtract(7L, "Q1 records", 100L, ArchiveStandard.EARK, sourceIntakeId,
                Map.of("id", 7L, "title", "Q1 records"),
                new PackagePayload(json, "eark_aip.json", "application/json"));
    }

    @Test
    void composesStandardMetadataPackageSourceAndArchive() {
        when(preservations.extract(7L)).thenReturn(Optional.of(eark(3L)));
        when(intakes.archiveIdOf(3L)).thenReturn(Optional.of(42L));
        when(archives.loadForExport(42L)).thenReturn(new ArchiveExportInfo(
                42L, 100L, "Finance", "EARK", "EARK",
                Map.of("id", 42L, "title", "Finance", "standardReference", "E-ARK CSIP 2.1.0")));

        Map<String, Object> doc = service.compose(7L).orElseThrow();

        assertThat(doc.keySet()).containsExactly("extract", "preservation", "package", "sourceIntake", "archive");
        Map<String, Object> extract = m(doc.get("extract"));
        assertThat(extract).containsEntry("type", "PRESERVATION").containsEntry("preservationId", 7L)
                .containsEntry("standard", "EARK").containsEntry("standardReference", "E-ARK CSIP 2.1.0")
                .containsKeys("standardName", "standardVersion", "extractedAt");
        assertThat(m(doc.get("preservation"))).containsEntry("title", "Q1 records");

        Map<String, Object> pkg = m(doc.get("package"));
        assertThat(pkg).containsEntry("filename", "eark_aip.json").containsEntry("contentType", "application/json");
        // JSON artifacts are embedded as structure, not as a string
        assertThat(m(m(pkg.get("content")).get("metsHeader"))).containsEntry("oaisPackageType", "AIP");

        assertThat(m(doc.get("sourceIntake"))).containsEntry("intakeId", 3L).containsEntry("archiveId", 42L);
        Map<String, Object> archive = m(doc.get("archive"));
        assertThat(archive).containsEntry("id", 42L).containsEntry("standard", "EARK");
        assertThat(m(archive.get("details"))).containsEntry("title", "Finance");
    }

    @Test
    void unlinkedPreservationHasNoSourceAndNullArchive() {
        when(preservations.extract(7L)).thenReturn(Optional.of(eark(null)));
        when(intakes.archiveIdOf(null)).thenReturn(Optional.empty());

        Map<String, Object> doc = service.compose(7L).orElseThrow();

        assertThat(doc).doesNotContainKey("sourceIntake");
        assertThat(doc).containsEntry("archive", null);
    }

    @Test
    void xmlArtifactsAreEmbeddedAsText() {
        byte[] xml = "<arkivuttrekk standardVersjon=\"5.5\"/>".getBytes(StandardCharsets.UTF_8);
        when(preservations.extract(9L)).thenReturn(Optional.of(new PreservationExtract(
                9L, "N5", 100L, ArchiveStandard.NOARK5, null, Map.of("id", 9L),
                new PackagePayload(xml, "arkivuttrekk_aip.xml", "application/xml"))));
        when(intakes.archiveIdOf(null)).thenReturn(Optional.empty());

        Map<String, Object> pkg = m(service.compose(9L).orElseThrow().get("package"));
        assertThat(pkg.get("content")).isEqualTo("<arkivuttrekk standardVersjon=\"5.5\"/>");
        assertThat(m(service.compose(9L).orElseThrow().get("extract")))
                .containsEntry("standardName", "Norsk Arkivstandard 5");
    }

    @Test
    void rendersJsonAndXmlDownloads() throws Exception {
        when(preservations.extract(7L)).thenReturn(Optional.of(eark(null)));
        when(intakes.archiveIdOf(null)).thenReturn(Optional.empty());

        var json = service.render(7L, PreservationExtractService.Format.JSON).orElseThrow();
        assertThat(json.filename()).isEqualTo("preservation_7_EARK_extract.json");
        assertThat(json.contentType()).isEqualTo("application/json");
        assertThat(json.tenantId()).isEqualTo(100L);
        Map<String, Object> parsed = new ObjectMapper().readValue(json.bytes(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        assertThat(parsed).containsKeys("extract", "preservation", "package");

        var xml = service.render(7L, PreservationExtractService.Format.XML).orElseThrow();
        assertThat(xml.filename()).isEqualTo("preservation_7_EARK_extract.xml");
        String text = new String(xml.bytes(), StandardCharsets.UTF_8);
        assertThat(text).contains("<preservationExtract>").contains("<oaisPackageType>AIP</oaisPackageType>")
                .contains("<standardReference>E-ARK CSIP 2.1.0</standardReference>");

        assertThat(service.render(99L, PreservationExtractService.Format.JSON)).isEmpty();
    }

    @Test
    void formatParsing() {
        assertThat(PreservationExtractService.Format.parse(null)).isEqualTo(PreservationExtractService.Format.JSON);
        assertThat(PreservationExtractService.Format.parse("XML")).isEqualTo(PreservationExtractService.Format.XML);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> PreservationExtractService.Format.parse("pdf"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("pdf");
        assertThat(List.of(PreservationExtractService.Format.values())).hasSize(2);
    }
}
