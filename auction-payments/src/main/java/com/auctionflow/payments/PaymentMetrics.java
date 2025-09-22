package com.auctionflow.payments;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Metrics for tracking payment operations.
 */
@Component
public class PaymentMetrics {

    private final Counter paymentsInitiated;
    private final Counter paymentsAuthorized;
    private final Counter paymentsCaptured;
    private final Counter paymentsSettled;
    private final Counter paymentsRefunded;
    private final Counter paymentsReleased;
    private final Counter totalRevenue;

    public PaymentMetrics(MeterRegistry meterRegistry) {
        this.paymentsInitiated = Counter.builder("payments_initiated_total")
                .description("Total number of payments initiated")
                .register(meterRegistry);
        this.paymentsAuthorized = Counter.builder("payments_authorized_total")
                .description("Total number of payments authorized")
                .register(meterRegistry);
        this.paymentsCaptured = Counter.builder("payments_captured_total")
                .description("Total number of payments captured")
                .register(meterRegistry);
        this.paymentsSettled = Counter.builder("payments_settled_total")
                .description("Total number of payments settled")
                .register(meterRegistry);
        this.paymentsRefunded = Counter.builder("payments_refunded_total")
                .description("Total number of payments refunded")
                .register(meterRegistry);
        this.paymentsReleased = Counter.builder("payments_released_total")
                .description("Total number of payments released")
                .register(meterRegistry);
        this.totalRevenue = Counter.builder("total_revenue")
                .description("Total revenue from settled payments")
                .register(meterRegistry);
    }

    public void incrementInitiated() {
        paymentsInitiated.increment();
    }

    public void incrementAuthorized() {
        paymentsAuthorized.increment();
    }

    public void incrementCaptured() {
        paymentsCaptured.increment();
    }

    public void incrementSettled() {
        paymentsSettled.increment();
    }

    public void incrementRefunded() {
        paymentsRefunded.increment();
    }

    public void incrementReleased() {
        paymentsReleased.increment();
    }

    public void incrementRevenue(double amount) {
        totalRevenue.increment(amount);
    }
}