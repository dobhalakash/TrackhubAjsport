package com.dreamnest.controller;

import com.dreamnest.dto.request.ChangePasswordRequest;
import com.dreamnest.dto.request.UpdateProfileRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.UserResponse;
import com.dreamnest.exception.UnauthorizedException;
import com.dreamnest.service.UserService;
import com.dreamnest.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for the authenticated user's own profile.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        Long userId = requireUserId();
        return ApiResponse.success(userService.getCurrentUser(userId));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = requireUserId();
        return ApiResponse.success("Profile updated successfully", userService.updateProfile(userId, request));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = requireUserId();
        userService.changePassword(userId, request);
        return ApiResponse.success("Password updated successfully", null);
    }

    private Long requireUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }
}
