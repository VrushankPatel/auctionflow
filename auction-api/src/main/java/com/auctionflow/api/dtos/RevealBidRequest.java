package com.auctionflow.api.dtos;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class RevealBidRequest {
    @NotNull
    @DecimalMin(value = "0.01", inclusive = false)
    private BigDecimal amount;
    @NotNull
    private String salt;
    private String idempotencyKey;

    public RevealBidRequest() {}

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}