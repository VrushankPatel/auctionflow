package com.auctionflow.api.config;

import com.auctionflow.api.services.AuctionSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.UUID;

@Component
public class AuctionVoter implements AccessDecisionVoter<Object> {

    @Autowired
    private AuctionSecurityService auctionSecurityService;

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return attribute.getAttribute().startsWith("AUCTION_");
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return true; // Support all classes
    }

    @Override
    public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
        if (authentication == null) {
            return ACCESS_DENIED;
        }

        UUID userId = (UUID) authentication.getPrincipal();

        for (ConfigAttribute attribute : attributes) {
            if ("AUCTION_CAN_BID".equals(attribute.getAttribute())) {
                if (object instanceof String) {
                    String auctionId = (String) object;
                    if (auctionSecurityService.canBid(auctionId, userId)) {
                        return ACCESS_GRANTED;
                    }
                }
            } else if ("AUCTION_CAN_EDIT".equals(attribute.getAttribute())) {
                if (object instanceof String) {
                    String auctionId = (String) object;
                    if (auctionSecurityService.canEdit(auctionId, userId)) {
                        return ACCESS_GRANTED;
                    }
                }
            }
        }

        return ACCESS_DENIED;
    }
}