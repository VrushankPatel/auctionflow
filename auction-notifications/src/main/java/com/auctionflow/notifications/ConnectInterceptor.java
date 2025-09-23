package com.auctionflow.notifications;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;
import java.util.Collections;
import org.springframework.stereotype.Component;

@Component
public class ConnectInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userId = (String) accessor.getSessionAttributes().get("userId");
            if (userId != null) {
                // Create a simple Authentication
                Authentication auth = new Authentication() {
                    @Override
                    public String getName() {
                        return userId;
                    }

                    @Override
                    public boolean isAuthenticated() {
                        return true;
                    }

                    @Override
                    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
                    }

                    @Override
                    public Object getCredentials() {
                        return null;
                    }

                    @Override
                    public Object getDetails() {
                        return null;
                    }

                    @Override
                    public Object getPrincipal() {
                        return userId;
                    }

                    @Override
                    public Collection<? extends GrantedAuthority> getAuthorities() {
                        return Collections.emptyList();
                    }

                    @Override
                    public boolean implies(javax.security.auth.Subject subject) {
                        return false;
                    }
                };
                accessor.setUser(auth);
            }
        }
        return message;
    }
}