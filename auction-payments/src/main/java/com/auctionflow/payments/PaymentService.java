package com.auctionflow.payments;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StateMachineFactory<Payment.PaymentStatus, PaymentEvent> stateMachineFactory;
    private final PaymentMetrics paymentMetrics;

    public PaymentService(PaymentRepository paymentRepository,
                          StateMachineFactory<Payment.PaymentStatus, PaymentEvent> stateMachineFactory,
                          PaymentMetrics paymentMetrics) {
        this.paymentRepository = paymentRepository;
        this.stateMachineFactory = stateMachineFactory;
        this.paymentMetrics = paymentMetrics;
    }

    @Transactional
    public Payment createPayment(String auctionId, String payerId, BigDecimal amount) {
        Payment payment = new Payment(auctionId, payerId, amount);
        Payment saved = paymentRepository.save(payment);
        paymentMetrics.incrementInitiated();
        return saved;
    }

    @Transactional
    public void authorizePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        StateMachine<Payment.PaymentStatus, PaymentEvent> sm = build(payment);
        sm.sendEvent(PaymentEvent.AUTHORIZE);
        Payment.PaymentStatus newStatus = sm.getState().getId();
        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        if (newStatus == Payment.PaymentStatus.AUTHORIZED) {
            paymentMetrics.incrementAuthorized();
        }
    }

    @Transactional
    public void capturePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        StateMachine<Payment.PaymentStatus, PaymentEvent> sm = build(payment);
        sm.sendEvent(PaymentEvent.CAPTURE);
        Payment.PaymentStatus newStatus = sm.getState().getId();
        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        if (newStatus == Payment.PaymentStatus.CAPTURED) {
            paymentMetrics.incrementCaptured();
        }
    }

    @Transactional
    public void releasePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        StateMachine<Payment.PaymentStatus, PaymentEvent> sm = build(payment);
        sm.sendEvent(PaymentEvent.RELEASE);
        Payment.PaymentStatus newStatus = sm.getState().getId();
        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        if (newStatus == Payment.PaymentStatus.RELEASED) {
            paymentMetrics.incrementReleased();
        }
    }

    @Transactional
    public void settlePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        StateMachine<Payment.PaymentStatus, PaymentEvent> sm = build(payment);
        sm.sendEvent(PaymentEvent.SETTLE);
        Payment.PaymentStatus newStatus = sm.getState().getId();
        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        if (newStatus == Payment.PaymentStatus.SETTLED) {
            paymentMetrics.incrementSettled();
            paymentMetrics.incrementRevenue(payment.getAmount().doubleValue());
        }
    }

    @Transactional
    public void refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        StateMachine<Payment.PaymentStatus, PaymentEvent> sm = build(payment);
        sm.sendEvent(PaymentEvent.REFUND);
        Payment.PaymentStatus newStatus = sm.getState().getId();
        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        if (newStatus == Payment.PaymentStatus.REFUNDED) {
            paymentMetrics.incrementRefunded();
        }
    }

    private StateMachine<Payment.PaymentStatus, PaymentEvent> build(Payment payment) {
        StateMachine<Payment.PaymentStatus, PaymentEvent> sm = stateMachineFactory.getStateMachine(payment.getId().toString());
        sm.getExtendedState().getVariables().put("paymentId", payment.getId());
        sm.getExtendedState().getVariables().put("auctionId", payment.getAuctionId());
        sm.getExtendedState().getVariables().put("payerId", payment.getPayerId());
        sm.getStateMachineAccessor().doWithAllRegions(access -> {
            access.resetStateMachine(new DefaultStateMachineContext<>(payment.getStatus(), null, null, null));
        });
        return sm;
    }
}