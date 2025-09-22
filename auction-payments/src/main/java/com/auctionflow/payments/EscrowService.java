package com.auctionflow.payments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EscrowService {

    @Autowired
    private EscrowRepository escrowRepository;

    @Autowired
    private PaymentGatewayService paymentGatewayService;

    // Assume provider is configured, e.g., "stripe"
    private static final String PAYMENT_PROVIDER = "stripe";
    private static final String CURRENCY = "USD";

    /**
     * Initiates escrow for a won auction.
     * Authorizes the payment and sets inspection period.
     */
    @Transactional
    public EscrowTransaction initiateEscrow(String auctionId, String winnerId, BigDecimal amount, String paymentMethodId) {
        // Authorize payment
        String authorizationId = paymentGatewayService.authorizePayment(PAYMENT_PROVIDER, amount, CURRENCY, paymentMethodId);

        // Create escrow transaction
        EscrowTransaction escrow = new EscrowTransaction(auctionId, winnerId, amount, authorizationId);
        escrow.setStatus(EscrowTransaction.EscrowStatus.INSPECTION);

        return escrowRepository.save(escrow);
    }

    /**
     * Confirms the escrow by the seller, capturing the payment.
     */
    @Transactional
    public void confirmEscrow(String auctionId) {
        EscrowTransaction escrow = escrowRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Escrow not found for auction: " + auctionId));

        if (escrow.getStatus() != EscrowTransaction.EscrowStatus.INSPECTION) {
            throw new IllegalStateException("Escrow is not in inspection state");
        }

        // Capture payment
        String captureId = paymentGatewayService.capturePayment(PAYMENT_PROVIDER, escrow.getAuthorizationId(), escrow.getAmount());
        escrow.setCaptureId(captureId);
        escrow.setStatus(EscrowTransaction.EscrowStatus.CAPTURED);

        escrowRepository.save(escrow);
    }

    /**
     * Processes expired inspections: captures payment on timeout.
     */
    @Transactional
    public void processExpiredInspections() {
        List<EscrowTransaction> expired = escrowRepository.findExpiredInspections(
                LocalDateTime.now(), EscrowTransaction.EscrowStatus.INSPECTION);

        for (EscrowTransaction escrow : expired) {
            // Capture payment on timeout
            String captureId = paymentGatewayService.capturePayment(PAYMENT_PROVIDER, escrow.getAuthorizationId(), escrow.getAmount());
            escrow.setCaptureId(captureId);
            escrow.setStatus(EscrowTransaction.EscrowStatus.CAPTURED);

            escrowRepository.save(escrow);
        }
    }

    /**
     * Cancels the escrow, e.g., if auction is cancelled.
     */
    @Transactional
    public void cancelEscrow(String auctionId) {
        EscrowTransaction escrow = escrowRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Escrow not found for auction: " + auctionId));

        if (escrow.getStatus() == EscrowTransaction.EscrowStatus.CAPTURED) {
            throw new IllegalStateException("Cannot cancel captured escrow");
        }

        // Optionally refund if authorized
        // For simplicity, just set to cancelled
        escrow.setStatus(EscrowTransaction.EscrowStatus.CANCELLED);

        escrowRepository.save(escrow);
    }

    /**
     * Puts the escrow in dispute, holding funds until resolution.
     */
    @Transactional
    public void disputeEscrow(String auctionId) {
        EscrowTransaction escrow = escrowRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Escrow not found for auction: " + auctionId));

        if (escrow.getStatus() != EscrowTransaction.EscrowStatus.INSPECTION) {
            throw new IllegalStateException("Escrow must be in inspection to dispute");
        }

        escrow.setStatus(EscrowTransaction.EscrowStatus.DISPUTE);

        escrowRepository.save(escrow);
    }

    /**
     * Resolves the dispute by capturing or cancelling the escrow.
     */
    @Transactional
    public void resolveDispute(String auctionId, boolean capture) {
        EscrowTransaction escrow = escrowRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Escrow not found for auction: " + auctionId));

        if (escrow.getStatus() != EscrowTransaction.EscrowStatus.DISPUTE) {
            throw new IllegalStateException("Escrow must be in dispute to resolve");
        }

        if (capture) {
            // Capture payment
            String captureId = paymentGatewayService.capturePayment(PAYMENT_PROVIDER, escrow.getAuthorizationId(), escrow.getAmount());
            escrow.setCaptureId(captureId);
            escrow.setStatus(EscrowTransaction.EscrowStatus.CAPTURED);
        } else {
            // Cancel/refund
            escrow.setStatus(EscrowTransaction.EscrowStatus.CANCELLED);
        }

        escrowRepository.save(escrow);
    }
}