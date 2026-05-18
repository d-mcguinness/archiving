package com.dmc.archiving.pkg.generator.noark5;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.ExtractInput;
import com.dmc.archiving.pkg.generator.noark5.Noark5XmlWriter.Stage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Noark5XmlWriterTest {

    @Test
    void sipExtractIsValidNoarkShapedXml() {
        Element root = new Element();
        root.setElementIdentifier("ARK-001");
        root.setEntityName("Personnel");
        root.setEntityType("Saksmappe");
        root.setNorwegianName("Personalmappe");
        root.setEnglishName("Personnel file");
        root.setTitle("Employee Records 2026");
        root.setDescription("Active personnel records");
        root.setCreatedAt(LocalDateTime.of(2026, 5, 18, 10, 0));
        root.setCreatedBy("david");
        root.setStatus("Active");

        Field f1 = new Field();
        f1.setName("documentMedium");
        f1.setValue("Elektronisk arkiv");
        Field f2 = new Field();
        f2.setName("archivePeriod");
        f2.setValue("2026-2030");
        root.setFields(List.of(f1, f2));

        byte[] raw = Noark5XmlWriter.write(new ExtractInput(
                Stage.SIP, 42L,
                "Q2 Personnel SIP", "Quarterly submission",
                "2026-05-18T10:00:00", "2026-05-18T10:15:00",
                "david", "DRAFT",
                root, null
        ));
        String xml = new String(Noark5XmlWriter.formatPretty(raw), StandardCharsets.UTF_8);

        assertThat(xml).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("xmlns=\"http://www.arkivverket.no/standarder/noark5/arkivstruktur\"");
        assertThat(xml).contains("standardVersjon=\"5.5\"");
        assertThat(xml).contains("uttrekkType=\"SIP\"");
        assertThat(xml).contains("<arkiv>");
        assertThat(xml).contains("<arkivdel>");
        assertThat(xml).contains("<systemID>ARK-001</systemID>");
        assertThat(xml).contains("<tittel>Employee Records 2026</tittel>");
        assertThat(xml).contains("<klassifikasjonssystem>");
        assertThat(xml).contains("<klasse>");
        assertThat(xml).contains("<klasseID>documentMedium</klasseID>");
        assertThat(xml).contains("<arkivstatus>Opprettet</arkivstatus>");
    }

    @Test
    void aipExtractHasBevartStatusAndAvlevertDate() {
        byte[] raw = Noark5XmlWriter.write(new ExtractInput(
                Stage.AIP, 7L, "Preserved", "desc",
                "2026-05-18T10:00:00", null, "david", "STORED",
                null, 42L
        ));
        String xml = new String(Noark5XmlWriter.formatPretty(raw), StandardCharsets.UTF_8);

        assertThat(xml).contains("uttrekkType=\"AIP\"");
        assertThat(xml).contains("<arkivstatus>Bevart</arkivstatus>");
        assertThat(xml).contains("<avlevertDato>2026-05-18T10:00:00</avlevertDato>");
        assertThat(xml).contains("<kildeIdentifikator>42</kildeIdentifikator>");
    }

    @Test
    void dipExtractMarksAvlevertStage() {
        byte[] raw = Noark5XmlWriter.write(new ExtractInput(
                Stage.DIP, 3L, "Delivered", "desc",
                "2026-05-18T10:00:00", null, "david", "DELIVERED",
                null, 7L
        ));
        String xml = new String(Noark5XmlWriter.formatPretty(raw), StandardCharsets.UTF_8);

        assertThat(xml).contains("uttrekkType=\"DIP\"");
        assertThat(xml).contains("<arkivstatus>Avlevert</arkivstatus>");
    }

    @Test
    void specialCharactersAreEscaped() {
        byte[] raw = Noark5XmlWriter.write(new ExtractInput(
                Stage.SIP, 1L,
                "Title with <tags> & ampersand",
                "Quotes \"like this\"",
                "2026-05-18", null, "user", "DRAFT",
                null, null
        ));
        String xml = new String(raw, StandardCharsets.UTF_8);

        assertThat(xml).contains("&lt;tags&gt;");
        assertThat(xml).contains("&amp;");
        assertThat(xml).doesNotContain("<tags>");
    }
}
