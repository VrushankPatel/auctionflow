package com.auctionflow.api.dtos;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

public class MakeOfferRequest {
    @NotNull
    private BigDecimal amount;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}