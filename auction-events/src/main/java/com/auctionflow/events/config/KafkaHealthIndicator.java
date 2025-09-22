package com.auctionflow.events.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Override
    public Health health() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            var clusterResult = adminClient.describeCluster(new DescribeClusterOptions().timeoutMs(5000));
            clusterResult.clusterId().get(5, TimeUnit.SECONDS);
            return Health.up().withDetail("kafka", "Available").build();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return Health.down(e).withDetail("kafka", "Unavailable").build();
        }
    }
}