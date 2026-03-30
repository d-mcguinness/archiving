package com.dmc.archiving.pkg.input;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreatePackageInput {
    private String tenantId;
    private String ownerId;
    private String userId;
    private String title;
    private String description;
    private String content;
    private String stage;       // SIP, AIP, DIP
    private String standard;    // NOARK5, OAIS, etc.
    private String sourceArchiveId;
    private String sourcePackageId;

    // Root element fields
    private String elementIdentifier;
    private String entityName;
    private String entityType;
    private String norwegianName;
    private String englishName;
    private String elementTitle;
    private String elementDescription;
    private String createdBy;
    private List<FieldInput> fields;

    @Getter
    @Setter
    public static class FieldInput {
        private String name;
        private String label;
        private String type;
        private String value;
    }
}
