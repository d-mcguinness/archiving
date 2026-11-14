package com.dmc.archiving.document;

import com.dmc.archiving.archive.api.ArchiveElementDepositApi;
import com.dmc.archiving.archive.api.ElementDeposit;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.document.DocumentArchiveForms.FormAnswer;
import com.dmc.archiving.document.DocumentArchiveForms.InvalidFormsException;
import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.scheme.api.EntityDefinition;
import com.dmc.archiving.scheme.api.FieldDefinition;
import com.dmc.archiving.scheme.api.FieldOption;
import com.dmc.archiving.scheme.api.SchemeDefinition;
import com.dmc.archiving.scheme.api.SchemeDefinitionApi;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Add Document wizard's forms are the only way a member writes into an
 * archive's element tree, so what gets through is checked against the
 * archive's standard: known entity, known fields, required answers, allowed
 * enumerated values.
 */
class DocumentArchiveFormsTest {

    private final SchemeDefinitionApi schemes = mock(SchemeDefinitionApi.class);
    private final ArchiveElementDepositApi depositApi = mock(ArchiveElementDepositApi.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private final DocumentArchiveForms forms = new DocumentArchiveForms(schemes, depositApi, documentService);

    private static final EntityDefinition FILE = new EntityDefinition(
            "File", "object_type", "A file", "Representation", List.of(), null, List.of(
                    new FieldDefinition("formatName", "Format Name", "string", true),
                    new FieldDefinition("algorithm", "Algorithm", "string", true, "algorithms",
                            List.of(new FieldOption("SHA-256", null), new FieldOption("MD5", null))),
                    new FieldDefinition("note", "Note", "text", false)));

    private Document document() {
        Document d = new Document();
        d.setId(7L);
        d.setTitle("Annual report");
        return d;
    }

    private void premisDefined() {
        when(schemes.get(ArchiveStandard.PREMIS)).thenReturn(Optional.of(new SchemeDefinition(
                ArchiveStandard.PREMIS, "PREMIS", "PREMIS", null, null, null, null, null,
                Map.of(), List.of(FILE), Map.of())));
    }

    private List<ElementDeposit> convert(FormAnswer... answers) {
        return forms.toElements("PREMIS", document(), List.of(answers));
    }

    @Test
    void mapsAFormToAnElementOfTheStandardsEntity() {
        premisDefined();

        List<ElementDeposit> elements = convert(
                new FormAnswer("File", Map.of("formatName", " text/plain ", "algorithm", "SHA-256")));

        assertThat(elements).hasSize(1);
        ElementDeposit e = elements.get(0);
        assertThat(e.entityName()).isEqualTo("File");
        assertThat(e.entityType()).isEqualTo("object_type");
        assertThat(e.parentEntityName()).isEqualTo("Representation");
        assertThat(e.title()).isEqualTo("Annual report");
        assertThat(e.elementIdentifier()).isEqualTo("DOC-7-1");
        assertThat(e.metadata()).isEqualTo("{\"documentId\":7}");
        // Every field of the definition is kept, in order, with labels and types from the definition.
        assertThat(e.fields()).extracting(ElementDeposit.Field::name).containsExactly("formatName", "algorithm", "note");
        assertThat(e.fields()).extracting(ElementDeposit.Field::value).containsExactly("text/plain", "SHA-256", "");
        assertThat(e.fields().get(0).label()).isEqualTo("Format Name");
    }

    @Test
    void noFormsMeansNoElementsAndNeedsNoDefinition() {
        assertThat(forms.toElements("PREMIS", document(), null)).isEmpty();
        assertThat(forms.toElements(null, document(), List.of())).isEmpty();
    }

    @Test
    void rejectsAnEntityTheStandardDoesNotHave() {
        premisDefined();
        assertThatThrownBy(() -> convert(new FormAnswer("Series", Map.of())))
                .isInstanceOf(InvalidFormsException.class).hasMessageContaining("Series");
    }

    @Test
    void rejectsAFieldTheEntityDoesNotDefine() {
        premisDefined();
        assertThatThrownBy(() -> convert(new FormAnswer("File",
                Map.of("formatName", "x", "algorithm", "MD5", "isRoot", "true"))))
                .isInstanceOf(InvalidFormsException.class).hasMessageContaining("isRoot");
    }

    @Test
    void rejectsAMissingRequiredAnswer() {
        premisDefined();
        assertThatThrownBy(() -> convert(new FormAnswer("File", Map.of("formatName", "  ", "algorithm", "MD5"))))
                .isInstanceOf(InvalidFormsException.class).hasMessageContaining("Format Name");
    }

    @Test
    void rejectsAValueOutsideTheVocabulary() {
        premisDefined();
        assertThatThrownBy(() -> convert(new FormAnswer("File", Map.of("formatName", "x", "algorithm", "ROT13"))))
                .isInstanceOf(InvalidFormsException.class).hasMessageContaining("ROT13");
    }

    @Test
    void rejectsMoreFormsThanADocumentCanNeed() {
        premisDefined();
        List<FormAnswer> many = Collections.nCopies(DocumentArchiveForms.MAX_FORMS + 1,
                new FormAnswer("File", Map.of("formatName", "x", "algorithm", "MD5")));
        assertThatThrownBy(() -> forms.toElements("PREMIS", document(), many))
                .isInstanceOf(InvalidFormsException.class);
    }

    @Test
    void rejectsFormsWhenTheStandardIsUnknown() {
        assertThatThrownBy(() -> forms.toElements("NOT_A_STANDARD", document(),
                List.of(new FormAnswer("File", Map.of()))))
                .isInstanceOf(InvalidFormsException.class);
    }

    @Test
    void addToArchiveDepositsElementsThenLinksTheDocument() {
        premisDefined();
        List<ElementDeposit> elements = convert(
                new FormAnswer("File", Map.of("formatName", "x", "algorithm", "MD5")));

        forms.addToArchive(7L, 55L, elements, "user");

        verify(depositApi).deposit(55L, elements, "user");
        verify(documentService).associateWithArchive(7L, 55L);
    }

    @Test
    void addToArchiveWithoutFormsOnlyLinks() {
        forms.addToArchive(7L, 55L, List.of(), "user");

        verify(depositApi, never()).deposit(anyLong(), anyList(), any());
        verify(documentService).associateWithArchive(7L, 55L);
    }
}
