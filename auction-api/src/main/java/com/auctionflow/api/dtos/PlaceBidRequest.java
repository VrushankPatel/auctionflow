package com.auctionflow.api.dtos;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

public class PlaceBidRequest {
    @NotNull
    @DecimalMin(value = "0.01", inclusive = false)
    private BigDecimal amount;
    private String idempotencyKey;

    public PlaceBidRequest() {}

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}