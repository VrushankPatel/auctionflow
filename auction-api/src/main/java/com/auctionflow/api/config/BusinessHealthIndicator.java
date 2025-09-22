package com.auctionflow.api.config;

import com.auctionflow.api.repositories.AuctionReadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class BusinessHealthIndicator implements HealthIndicator {

    @Autowired
    private AuctionReadRepository auctionReadRepository;

    @Override
    public Health health() {
        try {
            // Simple business logic check: count total auctions
            long totalAuctions = ((org.springframework.data.jpa.repository.JpaRepository) auctionReadRepository).count();
            return Health.up()
                .withDetail("totalAuctions", totalAuctions)
                .withDetail("businessLogic", "Operational")
                .build();
        } catch (Exception e) {
            return Health.down(e).withDetail("businessLogic", "Error accessing auction data").build();
        }
    }
}