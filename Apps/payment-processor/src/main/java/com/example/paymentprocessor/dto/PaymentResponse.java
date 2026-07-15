package com.example.paymentprocessor.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class PaymentResponse implements Serializable {

    private String transactionId;
    private String status;
    private String message;
    private String processedBy;
    private BigDecimal amount;
    private String currency;
    private Instant processedAt;

    public PaymentResponse() {
    }

    public PaymentResponse(String transactionId, String status, String message, String processedBy,
                            BigDecimal amount, String currency, Instant processedAt) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
        this.processedBy = processedBy;
        this.amount = amount;
        this.currency = currency;
        this.processedAt = processedAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
