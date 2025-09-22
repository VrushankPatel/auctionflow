package com.auctionflow.core.domain.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Optimized Money class for high-frequency bid processing.
 * Uses long internally (cents) for fast arithmetic and comparisons, minimizing BigDecimal overhead.
 * Provides toBigDecimal() for compatibility with persistence layers.
 */
public class Money {
    public static final Money ZERO = new Money(0L, Currency.getInstance("USD"));
    private final long amountCents; // Amount in cents for fast operations
    private final Currency currency;

    private Money(long amountCents, Currency currency) {
        if (amountCents < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        this.amountCents = amountCents;
        this.currency = currency;
    }

    public static Money usd(BigDecimal amount) {
        long cents = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
        return new Money(cents, Currency.getInstance("USD"));
    }

    public static Money usd(long cents) {
        return new Money(cents, Currency.getInstance("USD"));
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies must match");
        }
        return new Money(amountCents + other.amountCents, currency);
    }

    public boolean isGreaterThan(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies must match");
        }
        return amountCents > other.amountCents;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies must match");
        }
        return amountCents >= other.amountCents;
    }

    public Money subtract(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies must match");
        }
        return new Money(amountCents - other.amountCents, currency);
    }

    public Money multiply(BigDecimal factor) {
        long newCents = (long) (amountCents * factor.doubleValue());
        return new Money(newCents, currency);
    }

    public Money divide(int divisor) {
        return new Money(amountCents / divisor, currency);
    }

    public boolean isLessThan(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies must match");
        }
        return amountCents < other.amountCents;
    }

    public boolean isLessThanOrEqual(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies must match");
        }
        return amountCents <= other.amountCents;
    }

    public boolean isEqual(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies must match");
        }
        return amountCents == other.amountCents;
    }

    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(amountCents, 2);
    }

    public long getAmountCents() {
        return amountCents;
    }

    public Currency getCurrency() {
        return currency;
    }
}