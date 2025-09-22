package com.auctionflow.notifications;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final String SECRET_KEY = "your-secret-key"; // Should be configurable
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_STRING = "Authorization";
    private static final int MAX_CONNECTIONS = 1000; // Configurable connection limit
    private static final AtomicInteger connectionCount = new AtomicInteger(0);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // Check connection limit
        if (connectionCount.get() >= MAX_CONNECTIONS) {
            return false; // Reject connection
        }

        String token = null;

        // Try to get token from query parameter
        String query = request.getURI().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    token = param.substring(6);
                    break;
                }
            }
        }

        // If not in query, try Authorization header
        if (token == null) {
            String authHeader = request.getHeaders().getFirst(HEADER_STRING);
            if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
                token = authHeader.substring(TOKEN_PREFIX.length());
            }
        }

        if (token != null) {
            try {
                Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
                Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
                // Optionally, put user info into attributes
                attributes.put("userId", claims.getSubject());
                connectionCount.incrementAndGet();
                return true;
            } catch (Exception e) {
                // Invalid token
                return false;
            }
        }

        // No token provided
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No-op
    }
}