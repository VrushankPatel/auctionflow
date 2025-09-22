package com.auctionflow.payments;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Service that uses the Strategy pattern to select and delegate to the appropriate PaymentGatewayAdapter.
 */
@Service
public class PaymentGatewayService {

    private final Map<String, PaymentGatewayAdapter> adapters = new HashMap<>();

    public PaymentGatewayService() {
        // Register adapters
        adapters.put("stripe", new StripePaymentGatewayAdapter());
        adapters.put("paypal", new PayPalPaymentGatewayAdapter());
    }

    public String authorizePayment(String provider, BigDecimal amount, String currency, String paymentMethodId) {
        PaymentGatewayAdapter adapter = adapters.get(provider.toLowerCase());
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }
        return adapter.authorizePayment(amount, currency, paymentMethodId);
    }

    public String capturePayment(String provider, String authorizationId, BigDecimal amount) {
        PaymentGatewayAdapter adapter = adapters.get(provider.toLowerCase());
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }
        return adapter.capturePayment(authorizationId, amount);
    }

    public String refundPayment(String provider, String captureId, BigDecimal amount) {
        PaymentGatewayAdapter adapter = adapters.get(provider.toLowerCase());
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }
        return adapter.refundPayment(captureId, amount);
    }
}