package com.maxwell.userregistration.dto;

public record MfaSetupResponse(
        String secret,
        // OTPAuth URL — scan with Google Authenticator or any TOTP app
        String qrCodeUrl
) {}
