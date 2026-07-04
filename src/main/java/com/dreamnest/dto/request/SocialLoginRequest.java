package com.dreamnest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Sent by the frontend after a successful Google/Facebook/Apple sign-in
 * widget interaction. {@code token} is the provider's ID token (Google,
 * Apple) or access token (Facebook) - never a password.
 */
public class SocialLoginRequest {

    @NotBlank(message = "Provider is required")
    @Pattern(regexp = "GOOGLE|FACEBOOK|APPLE", message = "Provider must be GOOGLE, FACEBOOK or APPLE")
    private String provider;

    @NotBlank(message = "Token is required")
    private String token;

    /** Only ever sent by Apple, and only on the user's very first authorization. */
    private String firstName;
    private String lastName;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
