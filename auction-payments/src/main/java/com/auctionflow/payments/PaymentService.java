package com.auctionflow.payments;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StateMachineFactory<Payment.PaymentStatus, PaymentEvent> stateMachineFactory;

    public PaymentService(PaymentRepository paymentRepository,
                          StateMachineFactory<Payment.PaymentStatus, PaymentEvent> stateMachineFactory) {
        this.paymentRepository = paymentRepository;
        this.stateMachineFactory = stateMachineFactory;
    }

    @Transactional
    public Payment createPayment(String auctionId, String payerId, BigDecimal amount) {
        Payment payment = new Payment(auctionId, payerId, amount);
        return paymentRepository.save(payment);
    }

    @Transactional
    public void authorizePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        StateMachine<Payment.PaymentStatus, PaymentEvent> sm = build(payment);
        sm.sendEvent(PaymentEvent.AUTHORIZE);
        payment.setStatus(sm.getState().getId());
        paymentRepository.save(payment);
    }

    @Transactional
    public void capturePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        StateMachine<Payment.PaymentStatus, PaymentEvent> sm = build(payment);
        sm.sendEvent(PaymentEvent.CAPTURE);
        payment.setStatus(sm.getState().getId());
        paymentRepository.save(payment);
    }

    @Transactional
    public void releasePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        StateMachine<Payment.PaymentStatus, PaymentEvent> sm = build(payment);
        sm.sendEvent(PaymentEvent.RELEASE);
        payment.setStatus(sm.getState().getId());
        paymentRepository.save(payment);
    }

    @Transactional
    public void settlePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        StateMachine<Payment.PaymentStatus, PaymentEvent> sm = build(payment);
        sm.sendEvent(PaymentEvent.SETTLE);
        payment.setStatus(sm.getState().getId());
        paymentRepository.save(payment);
    }

    @Transactional
    public void refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        StateMachine<Payment.PaymentStatus, PaymentEvent> sm = build(payment);
        sm.sendEvent(PaymentEvent.REFUND);
        payment.setStatus(sm.getState().getId());
        paymentRepository.save(payment);
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