package com.dmc.archiving.intake.generator;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.intake.generator.impl.Noark5IntakeGenerator;
import com.dmc.archiving.intake.model.Intake;
import com.dmc.archiving.intake.model.IntakeStatus;
import com.dmc.archiving.storage.CloudStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end through the shared base: entity → snapshot → Noark 5 XML → upload.
 * Root-element fields must reach the XML (they were dropped before the
 * snapshot carried a field list) and the key must follow sips/{id}/.
 */
class Noark5IntakeGeneratorTest {

    private final CloudStorageService storage = mock(CloudStorageService.class);
    private final Noark5IntakeGenerator generator = new Noark5IntakeGenerator(storage);

    @Test
    void generatesXmlWithFieldsUnderTheSipPrefix() throws Exception {
        Intake sip = new Intake();
        sip.setId(5L);
        sip.setTitle("Employee Records 2026");
        sip.setStatus(IntakeStatus.DRAFT);
        sip.setStandard(ArchiveStandard.NOARK5);
        sip.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        Element root = new Element();
        root.setElementIdentifier("ARK-001");
        root.setEntityName("arkiv");
        root.setNorwegianName("Arkiv");
        root.setCreatedBy("alice");
        root.addField(new Field(null, null, "dokumentmedium", "Medium", "string", "Elektronisk arkiv"));
        sip.setRootElement(root);
        when(storage.uploadBytes(org.mockito.ArgumentMatchers.any(byte[].class), anyString(), eq("application/xml")))
                .thenReturn("s3://x/sips/5/arkivuttrekk_sip.xml");

        String url = generator.generate(sip);

        assertThat(url).startsWith("s3://x/sips/5/");
        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).uploadBytes(bytes.capture(), key.capture(), eq("application/xml"));
        assertThat(key.getValue()).startsWith("sips/5/");
        String xml = new String(bytes.getValue(), StandardCharsets.UTF_8);
        assertThat(xml).contains("uttrekkType=\"SIP\"");
        assertThat(xml).contains("<tittel>Employee Records 2026</tittel>");
        assertThat(xml).contains("<klasseID>dokumentmedium</klasseID>");
        assertThat(xml).contains("<beskrivelse>Elektronisk arkiv</beskrivelse>");
    }

    @Test
    void supportedStandardIsDerivedFromItsName() {
        assertThat(generator.supportedStandard()).isEqualTo(ArchiveStandard.NOARK5);
        assertThat(generator.packageType()).isEqualTo("sip");
    }
}
