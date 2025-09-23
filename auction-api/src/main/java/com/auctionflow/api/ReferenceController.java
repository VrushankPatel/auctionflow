package com.auctionflow.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reference")
public class ReferenceController {

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategories() {
        // Placeholder categories
        List<Map<String, Object>> categories = Arrays.asList(
            Map.of("id", "electronics", "name", "Electronics", "parent", (Object) null),
            Map.of("id", "phones", "name", "Phones", "parent", "electronics"),
            Map.of("id", "art", "name", "Art", "parent", (Object) null)
        );
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/bid-increments")
    public ResponseEntity<List<Map<String, Object>>> getBidIncrements() {
        // Placeholder bid increments
        List<Map<String, Object>> increments = Arrays.asList(
            Map.of("minAmount", 0.0, "increment", 1.0),
            Map.of("minAmount", 100.0, "increment", 5.0),
            Map.of("minAmount", 1000.0, "increment", 10.0)
        );
        return ResponseEntity.ok(increments);
    }

    @GetMapping("/auction-types")
    public ResponseEntity<List<Map<String, Object>>> getAuctionTypes() {
        // Placeholder auction types
        List<Map<String, Object>> types = Arrays.asList(
            Map.of("id", "english_open", "name", "English Open"),
            Map.of("id", "dutch", "name", "Dutch"),
            Map.of("id", "sealed_bid", "name", "Sealed Bid"),
            Map.of("id", "reserve_price", "name", "Reserve Price"),
            Map.of("id", "buy_now", "name", "Buy Now")
        );
        return ResponseEntity.ok(types);
    }

    @GetMapping("/extension-policies")
    public ResponseEntity<List<Map<String, Object>>> getExtensionPolicies() {
        // Placeholder extension policies
        List<Map<String, Object>> policies = Arrays.asList(
            Map.of("id", "none", "name", "No Extension"),
            Map.of("id", "fixed_window", "name", "Fixed Window", "windowMinutes", 5),
            Map.of("id", "unlimited", "name", "Unlimited Extensions"),
            Map.of("id", "max_extensions", "name", "Max Extensions", "maxExtensions", 3)
        );
        return ResponseEntity.ok(policies);
    }
}