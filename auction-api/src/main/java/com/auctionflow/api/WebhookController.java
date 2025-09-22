package com.auctionflow.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

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