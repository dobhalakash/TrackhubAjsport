package com.dreamnest.service;

import com.dreamnest.dto.request.LoginRequest;
import com.dreamnest.dto.request.RefreshTokenRequest;
import com.dreamnest.dto.request.RegisterBusinessRequest;
import com.dreamnest.dto.request.RegisterCustomerRequest;
import com.dreamnest.dto.request.ResendOtpRequest;
import com.dreamnest.dto.request.SocialLoginRequest;
import com.dreamnest.dto.request.VerifyOtpRequest;
import com.dreamnest.dto.response.AuthResponse;

/**
 * Handles user registration, login, and token refresh.
 */
public interface AuthService {

    AuthResponse registerCustomer(RegisterCustomerRequest request);

    AuthResponse registerBusiness(RegisterBusinessRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void verifyEmail(VerifyOtpRequest request);

    void verifyMobile(VerifyOtpRequest request);

    void resendOtp(ResendOtpRequest request);

    /** Verifies a Google/Facebook/Apple sign-in and logs the user in, creating an account if needed. */
    AuthResponse socialLogin(SocialLoginRequest request);
}
