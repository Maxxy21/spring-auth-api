package com.maxwell.userregistration.controller;

import com.maxwell.userregistration.dto.MfaSetupResponse;
import com.maxwell.userregistration.dto.MfaToggleRequest;
import com.maxwell.userregistration.dto.UserProfileResponse;
import com.maxwell.userregistration.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getUsername()));
    }

    /**
     * Step 1: Generate a new TOTP secret and QR code URL.
     * The user scans the QR code with their authenticator app.
     */
    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.setupMfa(principal.getUsername()));
    }

    /**
     * Step 2: Confirm setup by submitting the first code from the authenticator app.
     */
    @PostMapping("/mfa/enable")
    public ResponseEntity<Map<String, String>> enableMfa(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MfaToggleRequest request) {
        userService.enableMfa(principal.getUsername(), request.totpCode());
        return ResponseEntity.ok(Map.of("message", "MFA enabled successfully."));
    }

    /**
     * Disable MFA — requires a valid TOTP code to confirm identity.
     */
    @PostMapping("/mfa/disable")
    public ResponseEntity<Map<String, String>> disableMfa(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MfaToggleRequest request) {
        userService.disableMfa(principal.getUsername(), request.totpCode());
        return ResponseEntity.ok(Map.of("message", "MFA disabled successfully."));
    }
}
