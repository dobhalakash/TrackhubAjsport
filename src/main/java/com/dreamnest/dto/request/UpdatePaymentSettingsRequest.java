package com.dreamnest.dto.request;

import jakarta.validation.constraints.Pattern;

/**
 * Request payload for a business admin to set up their payout details: UPI
 * (used for customer-facing QR/deep-links) and bank account (used by the
 * admin to actually pay them out via NEFT/IMPS/RTGS).
 */
public class UpdatePaymentSettingsRequest {

    @Pattern(regexp = "^$|^[\\w.\\-]{2,256}@[a-zA-Z][\\w.\\-]{1,64}$",
            message = "Enter a valid UPI ID, e.g. yourname@upi")
    private String upiId;

    private String bankAccountNumber;
    private String ifscCode;
    private String bankName;
    private String accountHolderName;

    public UpdatePaymentSettingsRequest() {
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }
}
