package com.dmc.archiving.archive.input;

import com.dmc.archiving.archive.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignUserInput {
    private Long archiveId;
    private Long userId;
    private UserRole role;
}
