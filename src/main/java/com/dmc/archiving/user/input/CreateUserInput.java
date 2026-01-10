package com.dmc.archiving.user.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserInput {
    private String name;
    private String email;
    private Integer age;
}
