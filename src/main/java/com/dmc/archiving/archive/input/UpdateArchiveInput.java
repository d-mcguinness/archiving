package com.dmc.archiving.archive.input;

import com.dmc.archiving.archive.model.ArchiveStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateArchiveInput {
    private String title;
    private String description;
    private String content;
    private ArchiveStatus status;
}

