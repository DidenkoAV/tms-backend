package com.test.system.controller.authorization;

import com.test.system.dto.authorization.auth.AuthenticationResponse;
import com.test.system.dto.authorization.auth.LoginRequest;
import com.test.system.dto.authorization.auth.RegisterRequest;
import com.test.system.dto.authorization.common.StatusResponse;
import com.test.system.dto.authorization.password.ResetPasswordRequest;
import com.test.system.dto.authorization.password.SetPasswordRequest;
import com.test.system.exceptions.security.RateLimitExceededException;
import com.test.system.service.authorization.user.UserAuthenticationService;
import com.test.system.service.security.RateLimitService;
import com.test.system.utils.logging.LoggingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import static com.test.system.utils.auth.AuthWebUtils.safeEmail;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Public Controller", description = "Public authentication endpoints (no auth required)")
public class AuthPublicController {

    private static final Logger log = LoggerFactory.getLogger(AuthPublicController.class);

    private final UserAuthenticationService authService;
    private final RateLimitService rateLimitService;

    @Operation(
            summary = "Register new user",
            description = """
                    Register a new user account. After registration, a verification email will be sent.
                    The user must verify their email before they can login.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "email": "user@example.com",
                                              "password": "SecurePass123!",
                                              "fullName": "John Doe"
                                            }
                                            """
                            )
                    )
            )
    )
    @PostMapping("/register")
    public ResponseEntity<StatusResponse> register(
            @RequestBody @Valid RegisterRequest req,
            HttpServletRequest request
    ) {
        // Rate limiting: 3 registrations per hour per IP
        if (!rateLimitService.allowRegister(request)) {
            throw new RateLimitExceededException("Too many registration attempts. Please try again later.");
        }

        String email = safeEmail(req.email());
        log.info("Registration attempt for email: {}", email);

        authService.registerNewUser(email, req.password(), req.fullName());

        LoggingUtils.logAuthEvent(log, "REGISTER", email, true);
        return ResponseEntity.ok(StatusResponse.ok());
    }

    @Operation(
            summary = "Verify email address",
            description = "Verify user email using the token sent via email"
    )
    @PostMapping("/verify")
    public ResponseEntity<Void> verify(@RequestParam("token") String token) {
        authService.verifyUserEmail(token);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Login and get JWT token",
            description = """
                    Authenticate with email and password to receive a JWT token.

                    **Steps to use the token in Swagger:**
                    1. Copy the token value from the response
                    2. Click the 🔓 Authorize button at the top of this page
                    3. Paste the token (without "Bearer " prefix)
                    4. Click Authorize, then Close
                    5. All authenticated endpoints will now work
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "email": "user@example.com",
                                              "password": "SecurePass123!"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login successful, JWT token returned",
                            content = @Content(
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                      "tokenType": "Bearer"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @RequestBody @Valid LoginRequest req,
            HttpServletRequest request
    ) {
        // Rate limiting: 5 attempts per minute per IP
        if (!rateLimitService.allowLogin(request)) {
            throw new RateLimitExceededException("Too many login attempts. Please try again later.");
        }

        String email = safeEmail(req.email());
        log.info("Login attempt for email: {}", email);

        try {
            ResponseEntity<AuthenticationResponse> response = authService.authenticateUser(email, req.password(), request);
            LoggingUtils.logAuthEvent(log, "LOGIN", email, true);
            return response;
        } catch (Exception e) {
            LoggingUtils.logAuthEvent(log, "LOGIN", email, false);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        log.info("Logout request");
        LoggingUtils.logAuthEvent(log, "LOGOUT", "user", true);
        return authService.logoutUser(request);
    }

    @PostMapping("/verification/resend")
    public ResponseEntity<StatusResponse> resend(@RequestParam("email") String email) {
        authService.resendEmailVerification(safeEmail(email));
        return ResponseEntity.ok(StatusResponse.sent());
    }

    @PostMapping("/password/request-reset")
    public ResponseEntity<StatusResponse> requestReset(@RequestParam("email") String email) {
        String safeEmail = safeEmail(email);

        // Rate limiting: 3 requests per hour per email
        if (!rateLimitService.allowPasswordReset(safeEmail)) {
            throw new RateLimitExceededException("Too many password reset requests. Please try again later.");
        }

        authService.initiatePasswordReset(safeEmail);
        return ResponseEntity.ok(StatusResponse.sent());
    }

    @PostMapping("/password/reset")
    public ResponseEntity<StatusResponse> reset(
            @RequestParam("token") String token,
            @RequestBody ResetPasswordRequest body
    ) {
        authService.completePasswordReset(token, body.newPassword());
        return ResponseEntity.ok(StatusResponse.ok());
    }

    @Operation(
            summary = "Set password for new user",
            description = """
                    Set password for a placeholder user (created during group invitation).
                    After setting the password, the user is automatically logged in and receives a JWT token.
                    """
    )
    @PostMapping("/password/set")
    public ResponseEntity<AuthenticationResponse> setPassword(
            @RequestBody @Valid SetPasswordRequest req,
            HttpServletRequest request
    ) {
        String email = safeEmail(req.email());
        log.info("Set password request for email: {}", email);

        ResponseEntity<AuthenticationResponse> response = authService.setPasswordAndLogin(email, req.password(), request);
        LoggingUtils.logAuthEvent(log, "SET_PASSWORD", email, true);
        return response;
    }
}
