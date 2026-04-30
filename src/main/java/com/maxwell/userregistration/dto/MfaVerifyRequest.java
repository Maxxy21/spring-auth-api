package com.maxwell.userregistration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
        @NotBlank(message = "Temp token is required")
        String tempToken,

        @NotBlank(message = "TOTP code is required")
        @Pattern(regexp = "\\d{6}", message = "TOTP code must be 6 digits")
        String totpCode
) {}
