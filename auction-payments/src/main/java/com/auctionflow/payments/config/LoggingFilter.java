package com.auctionflow.payments.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        response.setHeader("X-Correlation-ID", correlationId);

        long startTime = System.currentTimeMillis();

        logger.info("Request: {} {} from {}:{}",
            request.getMethod(),
            request.getRequestURI(),
            request.getRemoteAddr(),
            request.getRemotePort());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Response: {} {} - Status: {} - Duration: {}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);

            MDC.clear();
        }
    }
}