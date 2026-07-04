package com.dreamnest.service;

import com.dreamnest.dto.request.ChangePasswordRequest;
import com.dreamnest.dto.request.UpdateProfileRequest;
import com.dreamnest.dto.response.UserResponse;

/**
 * Manages the authenticated user's profile.
 */
public interface UserService {

    UserResponse getCurrentUser(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
