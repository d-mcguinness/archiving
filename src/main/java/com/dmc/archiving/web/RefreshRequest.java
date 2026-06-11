package com.dmc.archiving.web;

import lombok.Data;

/** Body for POST /api/auth/refresh and /api/auth/logout: the opaque refresh token. */
@Data
public class RefreshRequest {
    private String refreshToken;
}
