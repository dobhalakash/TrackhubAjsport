package com.dreamnest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ResendOtpRequest {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Channel is required")
    @Pattern(regexp = "EMAIL|MOBILE", message = "Channel must be EMAIL or MOBILE")
    private String channel;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
