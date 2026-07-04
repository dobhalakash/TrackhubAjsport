package com.dreamnest.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request payload for business account registration.
 */
public class RegisterBusinessRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 200, message = "Business name must be at most 200 characters")
    private String businessName;

    @NotBlank(message = "Owner name is required")
    @Size(max = 150, message = "Owner name must be at most 150 characters")
    private String ownerName;

    @NotBlank(message = "GST number is required")
    @Size(max = 30, message = "GST number must be at most 30 characters")
    private String gstNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobile;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    public RegisterBusinessRequest() {
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
