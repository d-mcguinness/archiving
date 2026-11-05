package com.dmc.archiving.export;

import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.tenancy.api.TenancyApi;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Download the extract of one preservation as JSON or XML.
 *
 * Authentication: the bearer token is checked by RestAuthInterceptor
 * (registered on /api/preservations/** in RestAuthConfig). Authorization: the
 * caller must be ADMIN or a member of the preservation's tenant. The password
 * in the body mirrors the archive extract's contract: it is required, but the
 * download is not encrypted with it yet (same open TODO as /api/archives/…/extract).
 */
@RestController
@RequestMapping("/api/preservations")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:4173", "http://localhost:5173"})
public class PreservationExtractController {

    private static final Logger log = LoggerFactory.getLogger(PreservationExtractController.class);
    static final String AUTH_CONTEXT = "authContext";

    private final PreservationExtractService extracts;
    private final TenancyApi tenancyApi;

    public PreservationExtractController(PreservationExtractService extracts, TenancyApi tenancyApi) {
        this.extracts = extracts;
        this.tenancyApi = tenancyApi;
    }

    @PostMapping("/{preservationId}/extract")
    public ResponseEntity<?> extract(@PathVariable Long preservationId,
                                     @RequestParam(name = "format", required = false) String format,
                                     @RequestBody(required = false) Map<String, String> body,
                                     HttpServletRequest request) {
        AuthContext ctx = caller(request);
        if (!ctx.isAuthenticated()) {
            return error(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String password = body != null ? body.get("password") : null;
        if (password == null || password.trim().isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Password is required");
        }
        PreservationExtractService.Format fmt;
        try {
            fmt = PreservationExtractService.Format.parse(format);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Optional<PreservationExtractService.Rendered> rendered = extracts.render(preservationId, fmt);
        if (rendered.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Preservation not found");
        }
        PreservationExtractService.Rendered r = rendered.get();
        if (!canAccess(ctx, r.tenantId())) {
            return error(HttpStatus.FORBIDDEN, "Access denied: not a member of tenant " + r.tenantId());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(r.contentType()));
        headers.setContentLength(r.bytes().length);
        headers.setContentDispositionFormData("attachment", r.filename());
        log.info("Extracted preservation {} as {} for user {} ({} bytes)",
                preservationId, fmt.extension, ctx.userId(), r.bytes().length);
        return ResponseEntity.ok().headers(headers).body(r.bytes());
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "error", message));
    }

    private static AuthContext caller(HttpServletRequest request) {
        AuthContext ctx = (AuthContext) request.getAttribute(AUTH_CONTEXT);
        return ctx != null ? ctx : AuthContext.ANONYMOUS;
    }

    private boolean canAccess(AuthContext ctx, Long tenantId) {
        return ctx.isAdmin() || (tenantId != null && tenancyApi.isUserInTenant(ctx.userId(), tenantId));
    }
}
