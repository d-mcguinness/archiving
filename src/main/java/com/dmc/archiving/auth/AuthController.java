package com.dmc.archiving.auth;

import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication Controller
 * Handles login and authentication with role-based access
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4173", "http://localhost:5173"})
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Default credentials with roles
    private static final Map<String, AuthCredentials> DEFAULT_CREDENTIALS = new HashMap<>();

    static {
        DEFAULT_CREDENTIALS.put("admin", new AuthCredentials("admin", "admin123", "ADMIN", "Administrator"));
        DEFAULT_CREDENTIALS.put("tenant", new AuthCredentials("tenant", "tenant123", "TENANT", "Tenant Manager"));
        DEFAULT_CREDENTIALS.put("user", new AuthCredentials("user", "user123", "USER", "Regular User"));
    }

    /**
     * Login endpoint
     * POST /api/auth/login
     *
     * @param loginRequest containing username and password
     * @return Authentication response with user info, role, and token
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login attempt for username: {}", loginRequest.getUsername());

            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            // Validate input
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", "Username is required"
                    ));
            }

            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", "Password is required"
                    ));
            }

            // Check against default credentials
            AuthCredentials credentials = DEFAULT_CREDENTIALS.get(username.toLowerCase());

            if (credentials != null && credentials.getPassword().equals(password)) {
                // Valid credentials
                String token = generateToken(username, credentials.getRole());

                log.info("Login successful for user: {} with role: {}", username, credentials.getRole());

                // Create user response
                Map<String, Object> user = new HashMap<>();
                user.put("id", getDefaultUserId(username));
                user.put("username", username);
                user.put("name", credentials.getName());
                user.put("email", username + "@archiving.com");
                user.put("role", credentials.getRole());

                // Create response
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("user", user);
                response.put("role", credentials.getRole());
                response.put("token", token);
                response.put("expiresIn", 3600); // 1 hour

                return ResponseEntity.ok(response);
            } else {
                // Invalid credentials
                log.warn("Login failed for username: {} - Invalid credentials", username);

                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "success", false,
                        "error", "Invalid username or password"
                    ));
            }

        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "An error occurred during login"
                ));
        }
    }

    /**
     * Logout endpoint
     * POST /api/auth/logout
     *
     * @return Success response
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            log.info("Logout request received");

            // In a real application, invalidate the token here
            // For this demo, we just return success

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
            ));

        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "An error occurred during logout"
                ));
        }
    }

    /**
     * Verify token endpoint
     * GET /api/auth/verify
     *
     * @param token Authorization token
     * @return Token validation result
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "success", false,
                        "error", "No token provided"
                    ));
            }

            // Remove "Bearer " prefix if present
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;

            // In a real application, validate JWT token here
            // For this demo, we just check if it's not empty

            return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", true,
                "message", "Token is valid"
            ));

        } catch (Exception e) {
            log.error("Token verification error: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Token verification failed"
                ));
        }
    }

    /**
     * Generate a simple token
     * In production, use JWT with proper signing
     */
    private String generateToken(String username, String role) {
        return "Bearer_" + username + "_" + role + "_" + UUID.randomUUID().toString();
    }

    /**
     * Get default user ID based on username
     */
    private Long getDefaultUserId(String username) {
        switch (username.toLowerCase()) {
            case "admin": return 1L;
            case "tenant": return 2L;
            case "user": return 3L;
            default: return 999L;
        }
    }

    /**
     * Inner class for authentication credentials
     */
    private static class AuthCredentials {
        private final String username;
        private final String password;
        private final String role;
        private final String name;

        public AuthCredentials(String username, String password, String role, String name) {
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
