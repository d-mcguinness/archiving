package com.dmc.archiving.web;

import com.dmc.archiving.auth.LoginRequest;
import com.dmc.archiving.auth.api.TokenSigner;
import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.user.api.UserApi;
import com.dmc.archiving.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication Controller — DB-backed login and public self-service signup.
 * Login verifies credentials against stored BCrypt hashes (user module); signup
 * creates a user, provisions a FREE-plan tenant they own, and logs them in.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:4173", "http://localhost:5173"})
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final TenancyApi tenancyApi;
    private final UserApi userApi;
    private final TokenSigner tokenSigner;

    public AuthController(TenancyApi tenancyApi, UserApi userApi, TokenSigner tokenSigner) {
        this.tenancyApi = tenancyApi;
        this.userApi = userApi;
        this.tokenSigner = tokenSigner;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();
            log.info("Login attempt for username: {}", username);

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Username is required"));
            }
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Password is required"));
            }

            Optional<User> authenticated = userApi.authenticate(username, password);
            if (authenticated.isEmpty()) {
                log.warn("Login failed for username: {} - Invalid credentials", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Invalid username or password"));
            }
            log.info("Login successful for user: {}", username);
            return ResponseEntity.ok(authSuccess(authenticated.get(), "Login successful"));
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "An error occurred during login"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            log.info("Registration attempt for username: {}", request.getUsername());
            // Self-service signups are always tenant owners on the FREE plan;
            // ADMIN (operator) is never obtainable via the public endpoint.
            User user = userApi.register(
                    request.getName(), request.getEmail(), request.getUsername(),
                    request.getPassword(), "TENANT");
            Long tenantId = tenancyApi.createTenantWithOwner(request.getOrganization(), user.getId());
            log.info("Registered user {} ({}) as owner of new tenant {}", user.getId(), user.getUsername(), tenantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(authSuccess(user, "Registration successful"));
        } catch (IllegalArgumentException e) {
            // Validation / duplicate username or email.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "An error occurred during registration"));
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

    /** Build the shared login/registration success body: signed token + user + first tenant. */
    private Map<String, Object> authSuccess(User user, String message) {
        String token = tokenSigner.issue(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> userBody = new HashMap<>();
        userBody.put("id", user.getId());
        userBody.put("username", user.getUsername());
        userBody.put("name", user.getName());
        userBody.put("email", user.getEmail());
        userBody.put("role", user.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("user", userBody);
        response.put("role", user.getRole());
        response.put("token", token);
        response.put("expiresIn", 3600);

        // Non-ADMIN accounts resolve to their (first) tenant for the UI to scope to.
        if (!"ADMIN".equals(user.getRole())) {
            List<Long> tenantIds = tenancyApi.getTenantIdsByUserId(user.getId());
            if (!tenantIds.isEmpty()) {
                Long tenantId = tenantIds.get(0);
                response.put("tenantId", tenantId);
                userBody.put("tenantId", tenantId);
            }
        }
        return response;
    }
}
