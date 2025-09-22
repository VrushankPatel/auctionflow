package com.auctionflow.payments;

import java.math.BigDecimal;

/**
 * Interface for payment gateway adapters using the Strategy pattern.
 */
public interface PaymentGatewayAdapter {

    /**
     * Authorizes a payment.
     * @param amount the amount to authorize
     * @param currency the currency code (e.g., "USD")
     * @param paymentMethodId the payment method identifier
     * @return authorization ID
     */
    String authorizePayment(BigDecimal amount, String currency, String paymentMethodId);

    /**
     * Captures an authorized payment.
     * @param authorizationId the authorization ID from authorizePayment
     * @param amount the amount to capture
     * @return capture ID
     */
    String capturePayment(String authorizationId, BigDecimal amount);

    /**
     * Refunds a captured payment.
     * @param captureId the capture ID from capturePayment
     * @param amount the amount to refund
     * @return refund ID
     */
    String refundPayment(String captureId, BigDecimal amount);
}