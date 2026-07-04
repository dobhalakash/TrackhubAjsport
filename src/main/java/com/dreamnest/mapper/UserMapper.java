package com.dreamnest.mapper;

import com.dreamnest.dto.response.UserResponse;
import com.dreamnest.entity.User;

/**
 * Maps {@link User} entities to response DTOs.
 */
public class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.getRole() != null ? user.getRole().getName().name() : null,
                user.isEnabled(),
                user.isEmailVerified(),
                user.isMobileVerified()
        );
    }
}
