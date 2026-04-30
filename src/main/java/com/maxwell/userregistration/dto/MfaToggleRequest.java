package com.maxwell.userregistration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaToggleRequest(
        @NotBlank(message = "TOTP code is required")
        @Pattern(regexp = "\\d{6}", message = "TOTP code must be 6 digits")
        String totpCode
) {}
