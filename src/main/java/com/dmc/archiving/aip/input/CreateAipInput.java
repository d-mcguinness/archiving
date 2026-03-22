package com.dmc.archiving.aip.input;

import com.dmc.archiving.archive.model.ArchiveStandard;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAipInput {

    private Long tenantId;

    private Long ownerId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private String content;

    @NotNull(message = "Archive standard is required")
    private ArchiveStandard standard;

    private Long sourceSipId;

    // Inline root element fields
    @NotBlank(message = "Element identifier is required")
    private String elementIdentifier;

    @NotBlank(message = "Entity name is required")
    private String entityName;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    private String norwegianName;

    private String englishName;

    @NotBlank(message = "Element title is required")
    private String elementTitle;

    private String elementDescription;

    @NotBlank(message = "Created by is required")
    private String createdBy;

    private List<Map<String, Object>> fields;
}
