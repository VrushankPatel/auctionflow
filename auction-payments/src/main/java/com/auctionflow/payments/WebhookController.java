package com.auctionflow.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payments/webhook")
public class WebhookController {

    private final WebhookEventRepository webhookEventRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @Value("${payment.stripe.webhook.secret:}")
    private String stripeWebhookSecret;

    @Value("${payment.paypal.webhook.secret:}")
    private String paypalWebhookSecret;

    public WebhookController(WebhookEventRepository webhookEventRepository,
                             PaymentRepository paymentRepository,
                             PaymentService paymentService,
                             ObjectMapper objectMapper) {
        this.webhookEventRepository = webhookEventRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        if (!verifyStripeSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String eventId = jsonNode.get("id").asText();
            String eventType = jsonNode.get("type").asText();

            if (webhookEventRepository.findByEventId(eventId).isPresent()) {
                return ResponseEntity.ok().build(); // Idempotent
            }

            WebhookEvent webhookEvent = new WebhookEvent(eventId, "stripe", eventType, payload);
            webhookEventRepository.save(webhookEvent);

            handleStripeEvent(eventType, jsonNode);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/paypal")
    public ResponseEntity<Void> handlePaypalWebhook(
            @RequestBody String payload,
            @RequestHeader("PayPal-Transmission-Signature") String signature,
            @RequestHeader("PayPal-Transmission-Time") String transmissionTime,
            @RequestHeader("PayPal-Cert-Url") String certUrl,
            @RequestHeader("PayPal-Auth-Algo") String authAlgo) {

        // Simplified verification for PayPal (in real implementation, verify against cert)
        if (!verifyPaypalSignature(payload, signature, transmissionTime, certUrl, authAlgo)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String eventId = jsonNode.get("id").asText();
            String eventType = jsonNode.get("event_type").asText();

            if (webhookEventRepository.findByEventId(eventId).isPresent()) {
                return ResponseEntity.ok().build();
            }

            WebhookEvent webhookEvent = new WebhookEvent(eventId, "paypal", eventType, payload);
            webhookEventRepository.save(webhookEvent);

            handlePaypalEvent(eventType, jsonNode);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean verifyStripeSignature(String payload, String signature) {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isEmpty()) {
            return false;
        }

        try {
            String[] parts = signature.split(",");
            String timestamp = null;
            String sig = null;
            for (String part : parts) {
                if (part.startsWith("t=")) {
                    timestamp = part.substring(2);
                } else if (part.startsWith("v1=")) {
                    sig = part.substring(3);
                }
            }

            if (timestamp == null || sig == null) {
                return false;
            }

            String signedPayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(stripeWebhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String expectedSig = Base64.getEncoder().encodeToString(hash);

            return expectedSig.equals(sig);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }

    private boolean verifyPaypalSignature(String payload, String signature, String transmissionTime, String certUrl, String authAlgo) {
        // Simplified: in production, download cert from certUrl and verify signature
        return paypalWebhookSecret != null && !paypalWebhookSecret.isEmpty();
    }

    private void handleStripeEvent(String eventType, JsonNode jsonNode) {
        switch (eventType) {
            case "payment_intent.succeeded":
                handlePaymentAuthorized(jsonNode);
                break;
            case "payment_intent.payment_failed":
                // Handle failure
                break;
            case "charge.dispute.created":
                // Handle dispute
                break;
            // Add more events as needed
        }
    }

    private void handlePaypalEvent(String eventType, JsonNode jsonNode) {
        switch (eventType) {
            case "PAYMENT.CAPTURE.COMPLETED":
                handlePaymentCaptured(jsonNode);
                break;
            case "PAYMENT.CAPTURE.REFUNDED":
                handlePaymentRefunded(jsonNode);
                break;
            // Add more
        }
    }

    private void handlePaymentAuthorized(JsonNode jsonNode) {
        String providerRef = jsonNode.get("data").get("object").get("id").asText();
        Payment payment = paymentRepository.findByProviderRef(providerRef).orElse(null);
        if (payment != null) {
            paymentService.authorizePayment(payment.getId());
        }
    }

    private void handlePaymentCaptured(JsonNode jsonNode) {
        String providerRef = jsonNode.get("data").get("object").get("id").asText();
        Payment payment = paymentRepository.findByProviderRef(providerRef).orElse(null);
        if (payment != null) {
            paymentService.capturePayment(payment.getId());
            // Trigger fulfillment
            triggerFulfillment(payment.getId());
        }
    }

    private void handlePaymentRefunded(JsonNode jsonNode) {
        String providerRef = jsonNode.get("data").get("object").get("id").asText();
        Payment payment = paymentRepository.findByProviderRef(providerRef).orElse(null);
        if (payment != null) {
            paymentService.refundPayment(payment.getId());
        }
    }

    private void triggerFulfillment(Long paymentId) {
        // Publish event or call fulfillment service
        // For now, just log or assume integration
    }
}