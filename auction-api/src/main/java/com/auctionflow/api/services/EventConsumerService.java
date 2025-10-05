package com.auctionflow.api.services;

import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.events.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Map;

@Service
public class EventConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumerService.class);

    private final ApplicationContext applicationContext;

    public EventConsumerService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @KafkaListener(topics = "auction-events", groupId = "auction-api")
    public void consumeAuctionEvents(@Payload DomainEvent event) {
        logger.info("Received auction event: {}", event.getEventType());
        dispatchEvent(event);
    }

    @KafkaListener(topics = "bid-events", groupId = "auction-api")
    public void consumeBidEvents(@Payload DomainEvent event) {
        logger.info("Received bid event: {}", event.getEventType());
        dispatchEvent(event);
    }

    @KafkaListener(topics = "notification-events", groupId = "auction-api")
    public void consumeNotificationEvents(@Payload DomainEvent event) {
        logger.info("Received notification event: {}", event.getEventType());
        dispatchEvent(event);
    }

    private void dispatchEvent(DomainEvent event) {
        try {
            // Get all beans with EventHandler methods
            Map<String, Object> beans = applicationContext.getBeansWithAnnotation(org.springframework.stereotype.Component.class);

            for (Object bean : beans.values()) {
                Method[] methods = bean.getClass().getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(EventHandler.class)) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1 && parameterTypes[0].isAssignableFrom(event.getClass())) {
                            // Invoke the event handler
                            method.invoke(bean, event);
                            logger.debug("Dispatched event {} to handler {}", event.getEventType(), method.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to dispatch event {}", event.getEventType(), e);
        }
    }
}