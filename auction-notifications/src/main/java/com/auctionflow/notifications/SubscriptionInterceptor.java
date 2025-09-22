package com.auctionflow.notifications;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionInterceptor implements ChannelInterceptor {

    private final StringRedisTemplate redisTemplate;

    public SubscriptionInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            String user = accessor.getUser() != null ? accessor.getUser().getName() : null;
            if (destination != null && user != null && destination.startsWith("/user/queue/auctions/")) {
                // Extract auctionId from /user/queue/auctions/{id}
                String[] parts = destination.split("/");
                if (parts.length >= 4) {
                    String auctionId = parts[3];
                    // Add user to watchers
                    String key = "watchers:auction:" + auctionId;
                    redisTemplate.opsForSet().add(key, user);
                }
            }
        } else if (StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            String user = accessor.getUser() != null ? accessor.getUser().getName() : null;
            if (destination != null && user != null && destination.startsWith("/user/queue/auctions/")) {
                String[] parts = destination.split("/");
                if (parts.length >= 4) {
                    String auctionId = parts[3];
                    // Remove user from watchers
                    String key = "watchers:auction:" + auctionId;
                    redisTemplate.opsForSet().remove(key, user);
                }
            }
        }
        return message;
    }
}