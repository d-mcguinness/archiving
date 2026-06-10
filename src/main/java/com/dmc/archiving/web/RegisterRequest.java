package com.dmc.archiving.web;

import lombok.Data;

/** Self-service signup payload. */
@Data
public class RegisterRequest {
    private String name;
    private String organization;
    private String email;
    private String username;
    private String password;
}
