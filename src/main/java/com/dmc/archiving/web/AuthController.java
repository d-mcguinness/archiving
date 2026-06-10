package com.dmc.archiving.web;

import com.dmc.archiving.auth.LoginRequest;
import com.dmc.archiving.auth.api.TokenSigner;
import com.dmc.archiving.tenancy.api.TenancyApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication Controller
 * Handles login and authentication with role-based access
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:4173", "http://localhost:5173"})
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final TenancyApi tenancyApi;
    private final TokenSigner tokenSigner;

    public AuthController(TenancyApi tenancyApi, TokenSigner tokenSigner) {
        this.tenancyApi = tenancyApi;
        this.tokenSigner = tokenSigner;
    }

    private static final Map<String, AuthCredentials> DEFAULT_CREDENTIALS = new HashMap<>();

    static {
        DEFAULT_CREDENTIALS.put("admin", new AuthCredentials("admin", "admin123", "ADMIN", "Administrator"));
        DEFAULT_CREDENTIALS.put("tenant", new AuthCredentials("tenant", "tenant123", "TENANT", "Tenant Manager"));
        DEFAULT_CREDENTIALS.put("user", new AuthCredentials("user", "user123", "USER", "Regular User"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login attempt for username: {}", loginRequest.getUsername());

            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Username is required"));
            }

            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Password is required"));
            }

            AuthCredentials credentials = DEFAULT_CREDENTIALS.get(username.toLowerCase());

            if (credentials != null && credentials.getPassword().equals(password)) {
                String token = generateToken(username, credentials.getRole());

                log.info("Login successful for user: {} with role: {}", username, credentials.getRole());

                Long userId = getDefaultUserId(username);

                Map<String, Object> user = new HashMap<>();
                user.put("id", userId);
                user.put("username", username);
                user.put("name", credentials.getName());
                user.put("email", username + "@archiving.com");
                user.put("role", credentials.getRole());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("user", user);
                response.put("role", credentials.getRole());
                response.put("token", token);
                response.put("expiresIn", 3600);

                if ("TENANT".equals(credentials.getRole()) || "USER".equals(credentials.getRole())) {
                    try {
                        List<Long> tenantIds = tenancyApi.getTenantIdsByUserId(userId);
                        if (!tenantIds.isEmpty()) {
                            Long tenantId = tenantIds.get(0);
                            response.put("tenantId", tenantId);
                            user.put("tenantId", tenantId);
                            log.info("Added tenantId {} for user {} with role {}", tenantId, username, credentials.getRole());
                        } else {
                            log.warn("No tenants found for user {} with role {}", username, credentials.getRole());
                        }
                    } catch (Exception e) {
                        log.error("Error getting tenant IDs for user {}: {}", username, e.getMessage());
                    }
                }

                return ResponseEntity.ok(response);
            } else {
                log.warn("Login failed for username: {} - Invalid credentials", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Invalid username or password"));
            }
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "An error occurred during login"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        log.info("Logout request received");
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "error", "No token provided"));
        }
        // Actually verify the signature — a non-empty header is NOT proof of a
        // valid token. Reject forged/tampered tokens so this endpoint cannot
        // drive a client into an authenticated state with a bogus token.
        if (!tokenSigner.verify(token).isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "valid", false, "error", "Invalid token"));
        }
        return ResponseEntity.ok(Map.of("success", true, "valid", true, "message", "Token is valid"));
    }

    private String generateToken(String username, String role) {
        return tokenSigner.issue(username, role);
    }

    private Long getDefaultUserId(String username) {
        return switch (username.toLowerCase()) {
            case "admin" -> 1L;
            case "tenant" -> 2L;
            case "user" -> 3L;
            default -> 999L;
        };
    }

    private static class AuthCredentials {
        private final String username;
        private final String password;
        private final String role;
        private final String name;

        AuthCredentials(String username, String password, String role, String name) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.name = name;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
        public String getName() { return name; }
    }
}
