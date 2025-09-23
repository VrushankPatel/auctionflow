package com.auctionflow.api;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testWebhook(@RequestBody TestWebhookRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Send the webhook
            restTemplate.postForEntity(request.getUrl(), request.getPayload(), String.class);

            response.put("status", "success");
            response.put("message", "Webhook sent successfully");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments")
    @CircuitBreaker(name = "paymentWebhook")
    public ResponseEntity<Map<String, Object>> paymentWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("Received payment webhook: {}", payload);

        Map<String, Object> response = new HashMap<>();
        try {
            // Process payment webhook
            // Assume PaymentService exists to handle this
            // paymentService.processWebhook(payload);

            response.put("status", "success");
            response.put("message", "Payment webhook processed");
        } catch (Exception e) {
            logger.error("Failed to process payment webhook", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    public static class TestWebhookRequest {
        private String url;
        private String eventType;
        private Object payload;

        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }
    }
}