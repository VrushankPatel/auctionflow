package com.auctionflow.payments.config;

import com.auctionflow.payments.Payment;
import com.auctionflow.payments.PaymentEvent;
import com.auctionflow.payments.events.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
public class PaymentStateMachineConfig extends EnumStateMachineConfigurerAdapter<Payment.PaymentStatus, PaymentEvent> {

    private final ApplicationEventPublisher eventPublisher;

    public PaymentStateMachineConfig(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void configure(StateMachineStateConfigurer<Payment.PaymentStatus, PaymentEvent> states) throws Exception {
        states.withStates()
                .initial(Payment.PaymentStatus.PENDING)
                .states(EnumSet.allOf(Payment.PaymentStatus.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<Payment.PaymentStatus, PaymentEvent> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(Payment.PaymentStatus.PENDING).target(Payment.PaymentStatus.AUTHORIZED).event(PaymentEvent.AUTHORIZE)
                    .action(authorizeAction())
                .and()
                .withExternal()
                    .source(Payment.PaymentStatus.AUTHORIZED).target(Payment.PaymentStatus.CAPTURED).event(PaymentEvent.CAPTURE)
                    .action(captureAction())
                .and()
                .withExternal()
                    .source(Payment.PaymentStatus.AUTHORIZED).target(Payment.PaymentStatus.RELEASED).event(PaymentEvent.RELEASE)
                    .action(releaseAction())
                .and()
                .withExternal()
                    .source(Payment.PaymentStatus.CAPTURED).target(Payment.PaymentStatus.SETTLED).event(PaymentEvent.SETTLE)
                    .action(settleAction())
                .and()
                .withExternal()
                    .source(Payment.PaymentStatus.RELEASED).target(Payment.PaymentStatus.REFUNDED).event(PaymentEvent.REFUND)
                    .action(refundAction());
    }

    private Action<Payment.PaymentStatus, PaymentEvent> authorizeAction() {
        return context -> {
            Long paymentId = (Long) context.getExtendedState().getVariables().get("paymentId");
            String auctionId = (String) context.getExtendedState().getVariables().get("auctionId");
            String payerId = (String) context.getExtendedState().getVariables().get("payerId");
            eventPublisher.publishEvent(new PaymentAuthorizedEvent(paymentId, auctionId, payerId));
        };
    }

    private Action<Payment.PaymentStatus, PaymentEvent> captureAction() {
        return context -> {
            Long paymentId = (Long) context.getExtendedState().getVariables().get("paymentId");
            String auctionId = (String) context.getExtendedState().getVariables().get("auctionId");
            String payerId = (String) context.getExtendedState().getVariables().get("payerId");
            eventPublisher.publishEvent(new PaymentCapturedEvent(paymentId, auctionId, payerId));
        };
    }

    private Action<Payment.PaymentStatus, PaymentEvent> releaseAction() {
        return context -> {
            Long paymentId = (Long) context.getExtendedState().getVariables().get("paymentId");
            String auctionId = (String) context.getExtendedState().getVariables().get("auctionId");
            String payerId = (String) context.getExtendedState().getVariables().get("payerId");
            eventPublisher.publishEvent(new PaymentReleasedEvent(paymentId, auctionId, payerId));
        };
    }

    private Action<Payment.PaymentStatus, PaymentEvent> settleAction() {
        return context -> {
            Long paymentId = (Long) context.getExtendedState().getVariables().get("paymentId");
            String auctionId = (String) context.getExtendedState().getVariables().get("auctionId");
            String payerId = (String) context.getExtendedState().getVariables().get("payerId");
            eventPublisher.publishEvent(new PaymentSettledEvent(paymentId, auctionId, payerId));
        };
    }

    private Action<Payment.PaymentStatus, PaymentEvent> refundAction() {
        return context -> {
            Long paymentId = (Long) context.getExtendedState().getVariables().get("paymentId");
            String auctionId = (String) context.getExtendedState().getVariables().get("auctionId");
            String payerId = (String) context.getExtendedState().getVariables().get("payerId");
            eventPublisher.publishEvent(new PaymentRefundedEvent(paymentId, auctionId, payerId));
        };
    }
}