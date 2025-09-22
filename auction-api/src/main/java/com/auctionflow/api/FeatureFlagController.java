package com.auctionflow.api;

import com.auctionflow.common.service.FeatureFlagService;
import io.getunleash.UnleashContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/feature-flags")
@PreAuthorize("hasRole('ADMIN')")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping("/{flagName}")
    public ResponseEntity<Map<String, Object>> getFlagStatus(@PathVariable String flagName,
                                                             @RequestParam(required = false) String userId) {
        UnleashContext context = userId != null ? UnleashContext.builder().userId(userId).build() : UnleashContext.builder().build();
        boolean enabled = featureFlagService.isEnabled(flagName, context);
        String variant = featureFlagService.getVariant(flagName, context);

        Map<String, Object> response = Map.of(
                "flagName", flagName,
                "enabled", enabled,
                "variant", variant,
                "userId", userId
        );

        return ResponseEntity.ok(response);
    }

    // Note: Actual flag management (enable/disable) is done via Unleash admin UI
    // This endpoint provides read-only governance info
}