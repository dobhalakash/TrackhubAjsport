package com.dreamnest.controller;

import com.dreamnest.dto.request.LoginRequest;
import com.dreamnest.dto.request.RefreshTokenRequest;
import com.dreamnest.dto.request.RegisterBusinessRequest;
import com.dreamnest.dto.request.RegisterCustomerRequest;
import com.dreamnest.dto.request.ResendOtpRequest;
import com.dreamnest.dto.request.SocialLoginRequest;
import com.dreamnest.dto.request.VerifyOtpRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.AuthResponse;
import com.dreamnest.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for registration, login, and token refresh.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<AuthResponse>> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        AuthResponse response = authService.registerCustomer(request);
        return new ResponseEntity<>(ApiResponse.success(
                "Account created successfully. We've sent verification codes to your email and mobile number.", response), HttpStatus.CREATED);
    }

    @PostMapping("/register/business")
    public ResponseEntity<ApiResponse<AuthResponse>> registerBusiness(@Valid @RequestBody RegisterBusinessRequest request) {
        AuthResponse response = authService.registerBusiness(request);
        return new ResponseEntity<>(ApiResponse.success(
                "Business account created successfully. Your account is pending approval by the admin. " +
                        "We've also sent verification codes to your email and mobile number.", response), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/social-login")
    public ResponseEntity<ApiResponse<AuthResponse>> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        AuthResponse response = authService.socialLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Signed in successfully", response));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/verify-mobile")
    public ResponseEntity<ApiResponse<Void>> verifyMobile(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyMobile(request);
        return ResponseEntity.ok(ApiResponse.success("Mobile number verified successfully", null));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Verification code resent", null));
    }
}
