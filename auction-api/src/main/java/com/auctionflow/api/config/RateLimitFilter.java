package com.auctionflow.api.config;

import com.auctionflow.events.publisher.KafkaEventPublisher;
import com.auctionflow.core.domain.events.RateLimitExceededEvent;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final KafkaEventPublisher eventPublisher;
    private final Map<String, RateLimiter> ipRateLimiters = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitFilter(RateLimiterRegistry rateLimiterRegistry, KafkaEventPublisher eventPublisher) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ipAddress = getClientIpAddress(request);
        String endpoint = request.getRequestURI();

        // Create or get rate limiter for this IP
        RateLimiter rateLimiter = ipRateLimiters.computeIfAbsent(ipAddress, this::createRateLimiterForIp);

        if (rateLimiter.acquirePermission()) {
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            publishRateLimitEvent(request, ipAddress, endpoint);
            response.setStatus(429);
            response.getWriter().write("Too many requests");
        }
    }

    private RateLimiter createRateLimiterForIp(String ip) {
        return RateLimiter.of(ip, rateLimiterRegistry.getConfiguration("ipRateLimiter").orElseThrow());
    }

    private void publishRateLimitEvent(HttpServletRequest request, String ipAddress, String endpoint) {
        String userAgent = request.getHeader("User-Agent");
        RateLimitExceededEvent event = new RateLimitExceededEvent(
            java.util.UUID.randomUUID(),
            java.time.Instant.now(),
            ipAddress,
            userAgent,
            endpoint,
            "IP_RATE_LIMIT",
            Map.of("method", request.getMethod())
        );
        eventPublisher.publishSecurityEvent(event);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}