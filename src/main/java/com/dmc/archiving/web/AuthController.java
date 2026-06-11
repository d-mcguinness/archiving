package com.dmc.archiving.web;

import com.dmc.archiving.auth.LoginRequest;
import com.dmc.archiving.auth.RefreshTokenService;
import com.dmc.archiving.auth.api.TokenSigner;
import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.user.api.UserApi;
import com.dmc.archiving.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RegistrationService registrationService;
    private final SignupRateLimiter signupRateLimiter;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final RefreshTokenService refreshTokenService;

    public AuthController(TenancyApi tenancyApi, UserApi userApi, TokenSigner tokenSigner,
                          RegistrationService registrationService, SignupRateLimiter signupRateLimiter,
                          LoginAttemptLimiter loginAttemptLimiter, RefreshTokenService refreshTokenService) {
        this.tenancyApi = tenancyApi;
        this.userApi = userApi;
        this.tokenSigner = tokenSigner;
        this.registrationService = registrationService;
        this.signupRateLimiter = signupRateLimiter;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest httpRequest) {
        try {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();
            log.info("Login attempt for username: {}", username);

            // Throttle brute-force/spraying: block before verifying the password when
            // too many recent FAILURES exist for this account OR this client IP.
            String userKey = loginKey(username);
            String ipKey = "ip:" + clientIp(httpRequest);
            if (loginAttemptLimiter.isBlocked(userKey) || loginAttemptLimiter.isBlocked(ipKey)) {
                log.warn("Login throttled for username '{}' / {}", username, ipKey);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("success", false, "error", "Too many failed login attempts. Please try again later."));
            }

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
                loginAttemptLimiter.recordFailure(userKey);
                loginAttemptLimiter.recordFailure(ipKey);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Invalid username or password"));
            }
            log.info("Login successful for user: {}", username);
            loginAttemptLimiter.recordSuccess(userKey);
            loginAttemptLimiter.recordSuccess(ipKey);
            User user = authenticated.get();
            return ResponseEntity.ok(authSuccess(user, "Login successful",
                    refreshTokenService.generate(user.getId())));
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "An error occurred during login"));
        }
    }

    /** Lowercased, prefixed username key for the login limiter (null-safe). */
    private static String loginKey(String username) {
        return "u:" + (username == null ? "" : username.trim().toLowerCase());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        // Rate-limit per client IP before any work, to blunt automated signup abuse.
        String clientIp = clientIp(httpRequest);
        if (!signupRateLimiter.tryAcquire(clientIp)) {
            log.warn("Signup rate limit exceeded for {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("success", false, "error", "Too many sign-up attempts. Please try again later."));
        }
        try {
            log.info("Registration attempt for username: {}", request.getUsername());
            // One atomic step (RegistrationService): user + their FREE tenant, so a
            // provisioning failure rolls back the user rather than orphaning it.
            User user = registrationService.register(
                    request.getName(), request.getEmail(), request.getUsername(),
                    request.getPassword(), request.getOrganization());
            log.info("Registered user {} ({}) as a new tenant owner", user.getId(), user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(authSuccess(user, "Registration successful",
                    refreshTokenService.generate(user.getId())));
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

    /**
     * Exchange a valid refresh token for a fresh access token (and a rotated
     * refresh token). Single-use: the presented refresh token is consumed, so a
     * replay is rejected and a reused (already-rotated) token revokes the whole
     * session set. The user is re-read so a current role/username is minted and a
     * since-deleted account cannot refresh. Open endpoint (outside the REST auth
     * interceptor) and takes no access token — it renews an EXPIRED session.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        var rotation = refreshTokenService.rotate(request == null ? null : request.getRefreshToken());
        if (rotation.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "error", "Invalid or expired refresh token"));
        }
        Optional<User> user = userApi.getUserById(rotation.get().userId());
        if (user.isEmpty()) {
            // Account deleted since the refresh token was issued — nothing to renew.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "error", "Account no longer exists"));
        }
        return ResponseEntity.ok(authSuccess(user.get(), "Token refreshed", rotation.get().refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) RefreshRequest request) {
        // Real revocation (device-scoped): kill the presented refresh token so it
        // can no longer renew the session. The access token remains valid until its
        // own (short) expiry — the documented bound on offline-verified tokens.
        if (request != null) {
            refreshTokenService.revoke(request.getRefreshToken());
        }
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

    /**
     * Build the shared login/registration/refresh success body: a fresh signed
     * access token, the opaque refresh token to renew it with, the user, and
     * their first tenant.
     */
    private Map<String, Object> authSuccess(User user, String message, String refreshToken) {
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
        response.put("refreshToken", refreshToken);
        response.put("expiresIn", tokenSigner.ttlSeconds());

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

    /** Client IP for rate-limiting — first X-Forwarded-For hop if present, else the socket address. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
