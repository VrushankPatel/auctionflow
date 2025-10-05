package com.auctionflow.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class AuctionWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuctionWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Map of auction ID to set of sessions subscribed to that auction
    private final Map<String, Set<WebSocketSession>> auctionSubscriptions = new ConcurrentHashMap<>();
    // Map of session to set of auction IDs it's subscribed to
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());
        sessionSubscriptions.put(session.getId(), new CopyOnWriteArraySet<>());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            
            String type = (String) data.get("type");
            String auctionId = (String) data.get("auctionId");
            
            if ("subscribe".equals(type) && auctionId != null) {
                subscribe(session, auctionId);
            } else if ("unsubscribe".equals(type) && auctionId != null) {
                unsubscribe(session, auctionId);
            }
        } catch (Exception e) {
            logger.error("Error handling WebSocket message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket connection closed: {}", session.getId());
        
        // Unsubscribe from all auctions
        Set<String> auctions = sessionSubscriptions.remove(session.getId());
        if (auctions != null) {
            for (String auctionId : auctions) {
                Set<WebSocketSession> sessions = auctionSubscriptions.get(auctionId);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        auctionSubscriptions.remove(auctionId);
                    }
                }
            }
        }
    }

    private void subscribe(WebSocketSession session, String auctionId) {
        auctionSubscriptions.computeIfAbsent(auctionId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionSubscriptions.get(session.getId()).add(auctionId);
        logger.info("Session {} subscribed to auction {}", session.getId(), auctionId);
    }

    private void unsubscribe(WebSocketSession session, String auctionId) {
        Set<WebSocketSession> sessions = auctionSubscriptions.get(auctionId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                auctionSubscriptions.remove(auctionId);
            }
        }
        sessionSubscriptions.get(session.getId()).remove(auctionId);
        logger.info("Session {} unsubscribed from auction {}", session.getId(), auctionId);
    }

    /**
     * Broadcast a message to all sessions subscribed to a specific auction
     */
    public void broadcastToAuction(String auctionId, Object message) {
        Set<WebSocketSession> sessions = auctionSubscriptions.get(auctionId);
        if (sessions != null) {
            String messageJson;
            try {
                messageJson = objectMapper.writeValueAsString(message);
            } catch (Exception e) {
                logger.error("Error serializing message", e);
                return;
            }
            
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                    } catch (IOException e) {
                        logger.error("Error sending message to session {}", session.getId(), e);
                    }
                }
            }
        }
    }
}
