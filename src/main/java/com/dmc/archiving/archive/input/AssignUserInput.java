package com.dmc.archiving.archive.input;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignUserInput {

    @NotNull(message = "Archive ID is required")
    private Long archiveId;

    @NotNull(message = "User ID is required")
    private Long userId;
}
