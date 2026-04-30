package com.maxwell.userregistration.controller;

import com.maxwell.userregistration.dto.*;
import com.maxwell.userregistration.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Register a new user account.
     * Requires a valid reCAPTCHA token (disabled in test profile).
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful. Check your email to verify your account."));
    }

    /**
     * Verify email address via the token sent by email.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now log in."));
    }

    /**
     * Resend the verification email.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        userService.resendVerificationEmail(request.email());
        return ResponseEntity.ok(Map.of("message", "Verification email resent."));
    }

    /**
     * Authenticate a user.
     * Returns an access + refresh token pair, or an MFA-pending response if MFA is enabled.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    /**
     * Complete login when MFA is enabled.
     * Requires the tempToken from /login and a 6-digit TOTP code.
     */
    @PostMapping("/mfa/validate")
    public ResponseEntity<AuthResponse> validateMfa(@Valid @RequestBody MfaVerifyRequest request) {
        return ResponseEntity.ok(userService.validateMfa(request));
    }

    /**
     * Refresh the access token using a valid refresh token (token rotation is applied).
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(userService.refreshToken(request));
    }

    /**
     * Revoke the current refresh token (logout).
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody TokenRefreshRequest request) {
        userService.logout(request.refreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }
}
