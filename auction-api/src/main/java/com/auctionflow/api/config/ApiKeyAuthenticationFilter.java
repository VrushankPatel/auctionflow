package com.auctionflow.api.config;

import com.auctionflow.api.services.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Profile("!min & !ui-only")
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Skip API key check for public URLs
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/ui/") ||
            requestURI.startsWith("/static/") ||
            requestURI.startsWith("/images/") ||
            requestURI.startsWith("/ws") ||
            requestURI.startsWith("/api/v1/auth/") ||
            requestURI.startsWith("/api/v1/reference/") ||
            requestURI.startsWith("/api/v1/users") ||
            requestURI.startsWith("/api/v1/auctions") ||
            requestURI.startsWith("/assets/") ||
            requestURI.equals("/favicon.ico") ||
            requestURI.equals("/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String apiKey = getApiKeyFromRequest(request);

            if (StringUtils.hasText(apiKey) && apiKeyService.validateKey(apiKey)) {
                // For service-to-service, set authentication with service role
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "service", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE")));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Log exception
        }

        filterChain.doFilter(request, response);
    }

    private String getApiKeyFromRequest(HttpServletRequest request) {
        return request.getHeader("X-API-Key");
    }
}