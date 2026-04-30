package com.maxwell.userregistration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        boolean mfaRequired,
        // Present only when mfaRequired = true; client uses it to call /auth/mfa/validate
        String tempToken
) {
    public static AuthResponse authenticated(String accessToken, String refreshToken, String email) {
        return new AuthResponse(accessToken, refreshToken, email, false, null);
    }

    public static AuthResponse mfaPending(String tempToken, String email) {
        return new AuthResponse(null, null, email, true, tempToken);
    }
}
