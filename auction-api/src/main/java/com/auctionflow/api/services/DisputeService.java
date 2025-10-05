package com.auctionflow.api.services;

import com.auctionflow.api.entities.Dispute;
import com.auctionflow.api.entities.DisputeEvidence;
import com.auctionflow.api.repositories.DisputeRepository;
import com.auctionflow.api.repositories.DisputeEvidenceRepository;
import com.auctionflow.core.domain.events.DisputeCreatedEvent;
import com.auctionflow.core.domain.events.DisputeResolvedEvent;
import com.auctionflow.events.publisher.KafkaEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("!ui-only")
public class DisputeService {

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private DisputeEvidenceRepository disputeEvidenceRepository;

    @Autowired
    private KafkaEventPublisher eventPublisher;

    @Transactional
    public Dispute createDispute(String auctionId, String initiatorId, String reason, String description) {
        // Check if there's already an open dispute for this auction
        Optional<Dispute> existing = disputeRepository.findOpenDisputeByAuctionId(auctionId);
        if (existing.isPresent()) {
            throw new IllegalStateException("An open dispute already exists for this auction");
        }

        Dispute dispute = new Dispute(auctionId, initiatorId, reason, description);

        Dispute saved = disputeRepository.save(dispute);

        // Publish event
        DisputeCreatedEvent event = new DisputeCreatedEvent(auctionId, initiatorId, reason, description, UUID.randomUUID(), Instant.now(), 0);
        eventPublisher.publish(event);

        // TODO: Call escrow service to put in dispute
        // escrowService.disputeEscrow(auctionId);

        return saved;
    }

    public Optional<Dispute> getDisputeById(Long id) {
        return disputeRepository.findById(id);
    }

    public List<Dispute> getDisputesByInitiator(String initiatorId) {
        return disputeRepository.findByInitiatorId(initiatorId);
    }

    public List<Dispute> getDisputesByStatus(String status) {
        return disputeRepository.findByStatus(status);
    }

    @Transactional
    public DisputeEvidence submitEvidence(Long disputeId, String submittedBy, String evidenceType, String content) {
        DisputeEvidence evidence = new DisputeEvidence(disputeId, submittedBy, evidenceType, content);
        return disputeEvidenceRepository.save(evidence);
    }

    public List<DisputeEvidence> getEvidenceForDispute(Long disputeId) {
        return disputeEvidenceRepository.findByDisputeId(disputeId);
    }

    @Transactional
    public Dispute resolveDispute(Long disputeId, String resolverId, String resolutionNotes, boolean captureEscrow) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));

        dispute.setStatus("RESOLVED");
        dispute.setResolvedAt(Instant.now());
        dispute.setResolverId(resolverId);
        dispute.setResolutionNotes(resolutionNotes);

        Dispute saved = disputeRepository.save(dispute);

        // Publish event
        DisputeResolvedEvent event = new DisputeResolvedEvent(disputeId, dispute.getAuctionId(), resolverId, resolutionNotes, captureEscrow, UUID.randomUUID(), Instant.now(), 0);
        eventPublisher.publish(event);

        // TODO: Call escrow service to resolve dispute
        // escrowService.resolveDispute(dispute.getAuctionId(), captureEscrow);

        return saved;
    }

    @Transactional
    public Dispute closeDispute(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));

        dispute.setStatus("CLOSED");
        return disputeRepository.save(dispute);
    }
}