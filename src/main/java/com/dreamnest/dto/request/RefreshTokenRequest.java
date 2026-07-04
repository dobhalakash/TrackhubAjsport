package com.dreamnest.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for obtaining a new access token using a refresh token.
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
