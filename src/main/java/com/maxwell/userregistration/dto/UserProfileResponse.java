package com.maxwell.userregistration.dto;

import com.maxwell.userregistration.model.Role;
import com.maxwell.userregistration.model.User;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean mfaEnabled,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isMfaEnabled(),
                user.getCreatedAt()
        );
    }
}
