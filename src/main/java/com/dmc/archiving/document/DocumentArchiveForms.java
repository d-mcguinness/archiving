package com.dmc.archiving.document;

import com.dmc.archiving.archive.api.ArchiveElementDepositApi;
import com.dmc.archiving.archive.api.ElementDeposit;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.scheme.api.EntityDefinition;
import com.dmc.archiving.scheme.api.FieldDefinition;
import com.dmc.archiving.scheme.api.SchemeDefinition;
import com.dmc.archiving.scheme.api.SchemeDefinitionApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns the forms filled in when a document is added to an archive (the Add
 * Document wizard) into elements of that archive's tree.
 *
 * <p>The forms are checked here against the archive's standard definition —
 * the entity must exist, every field must be one the entity defines, required
 * fields must be answered and enumerated fields must hold an allowed value —
 * so a member, who cannot edit the element tree directly, can only add
 * well-formed descriptions of their own document.
 */
@Service
public class DocumentArchiveForms {

    /** More forms than any standard asks for with one document; guards the tree against bulk inserts. */
    static final int MAX_FORMS = 25;

    /** One filled-in form: the standard's entity name and its field values by field name. */
    public record FormAnswer(String entity, Map<String, String> values) {
    }

    /** The forms are not valid for the archive's standard; the message is safe to show the user. */
    public static class InvalidFormsException extends RuntimeException {
        public InvalidFormsException(String message) {
            super(message);
        }
    }

    private final SchemeDefinitionApi schemes;
    private final ArchiveElementDepositApi depositApi;
    private final DocumentService documentService;

    public DocumentArchiveForms(SchemeDefinitionApi schemes, ArchiveElementDepositApi depositApi,
                                DocumentService documentService) {
        this.schemes = schemes;
        this.depositApi = depositApi;
        this.documentService = documentService;
    }

    /**
     * Validates the forms against the standard and maps each to the element it becomes.
     *
     * @throws InvalidFormsException naming the first problem found
     */
    public List<ElementDeposit> toElements(String standardName, Document document, List<FormAnswer> forms) {
        if (forms == null || forms.isEmpty()) {
            return List.of();
        }
        if (forms.size() > MAX_FORMS) {
            throw new InvalidFormsException("Too many forms: at most " + MAX_FORMS + " per document");
        }
        SchemeDefinition scheme = scheme(standardName);

        List<ElementDeposit> elements = new ArrayList<>();
        int n = 0;
        for (FormAnswer form : forms) {
            n++;
            if (form == null || form.entity() == null) {
                throw new InvalidFormsException("Form " + n + " does not name an entity");
            }
            EntityDefinition entity = scheme.entity(form.entity()).orElseThrow(() -> new InvalidFormsException(
                    "\"" + form.entity() + "\" is not a form of " + scheme.displayName()));
            Map<String, String> values = form.values() == null ? Map.of() : form.values();

            for (String name : values.keySet()) {
                if (entity.fields().stream().noneMatch(f -> f.name().equals(name))) {
                    throw new InvalidFormsException(entity.name() + ": unknown field \"" + name + "\"");
                }
            }
            List<ElementDeposit.Field> fields = new ArrayList<>();
            for (FieldDefinition field : entity.fields()) {
                String value = values.get(field.name());
                value = value == null ? "" : value.trim();
                if (value.isEmpty() && field.required()) {
                    throw new InvalidFormsException(
                            entity.name() + ": required field \"" + field.label() + "\" is empty");
                }
                if (!value.isEmpty() && field.isEnumerated()) {
                    String v = value;
                    if (field.options().stream().noneMatch(o -> o.value().equals(v))) {
                        throw new InvalidFormsException(
                                entity.name() + ": \"" + value + "\" is not an allowed value for \"" + field.label() + "\"");
                    }
                }
                fields.add(new ElementDeposit.Field(field.name(), field.label(), field.type(), value));
            }

            elements.add(new ElementDeposit(
                    entity.name(),
                    entity.type(),
                    entity.parent(),
                    "DOC-" + document.getId() + "-" + n,
                    document.getTitle(),
                    "{\"documentId\":" + document.getId() + "}",
                    fields));
        }
        return elements;
    }

    /** Adds the elements to the archive's tree and links the document to the archive, as one change. */
    @Transactional
    public Document addToArchive(Long documentId, Long archiveId, List<ElementDeposit> elements, String createdBy) {
        if (!elements.isEmpty()) {
            depositApi.deposit(archiveId, elements, createdBy);
        }
        return documentService.associateWithArchive(documentId, archiveId);
    }

    private SchemeDefinition scheme(String standardName) {
        try {
            return schemes.get(ArchiveStandard.valueOf(standardName)).orElseThrow();
        } catch (RuntimeException e) {
            throw new InvalidFormsException("This archive's standard has no forms defined");
        }
    }
}
