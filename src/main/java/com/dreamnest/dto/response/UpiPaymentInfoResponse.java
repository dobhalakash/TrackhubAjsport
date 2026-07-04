package com.dreamnest.dto.response;

import java.math.BigDecimal;

/**
 * Response payload describing how a customer can pay for an order via UPI
 * (PhonePe, Google Pay, Paytm, etc.). The frontend renders {@code upiUri} as
 * both a QR code and a tappable deep link - on a phone with a UPI app
 * installed, tapping the link automatically opens the app's payment screen.
 */
public class UpiPaymentInfoResponse {

    private Long orderId;
    private String orderNumber;
    private BigDecimal amount;
    private String payeeUpiId;
    private String payeeName;
    private String transactionNote;
    private String upiUri;
    private String paymentStatus;

    public UpiPaymentInfoResponse() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPayeeUpiId() {
        return payeeUpiId;
    }

    public void setPayeeUpiId(String payeeUpiId) {
        this.payeeUpiId = payeeUpiId;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getTransactionNote() {
        return transactionNote;
    }

    public void setTransactionNote(String transactionNote) {
        this.transactionNote = transactionNote;
    }

    public String getUpiUri() {
        return upiUri;
    }

    public void setUpiUri(String upiUri) {
        this.upiUri = upiUri;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
