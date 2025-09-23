package com.auctionflow.api.services;

import com.auctionflow.api.entities.*;
import com.auctionflow.api.repositories.*;
import com.auctionflow.events.persistence.EventEntity;
import com.auctionflow.events.persistence.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Service
public class ArchivalService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArchivedAuctionRepository archivedAuctionRepository;

    @Autowired
    private ArchivedBidRepository archivedBidRepository;

    @Autowired
    private ArchivedEventRepository archivedEventRepository;

    @Scheduled(fixedRate = 86400000) // Run daily
    @Transactional
    public void archiveOldAuctions() {
        Instant cutoff = Instant.now().minusSeconds(365L * 24 * 60 * 60); // 365 days ago

        List<Auction> oldAuctions = auctionRepository.findByStatusAndEndTsBefore("CLOSED", cutoff);

        for (Auction auction : oldAuctions) {
            // Move to archived_auctions
            ArchivedAuction archivedAuction = mapToArchivedAuction(auction);
            archivedAuctionRepository.save(archivedAuction);

            // Move bids
            List<Bid> bids = bidRepository.findByAuctionId(auction.getId());
            for (Bid bid : bids) {
                ArchivedBid archivedBid = mapToArchivedBid(bid);
                archivedBidRepository.save(archivedBid);
            }

            // Move events
            // List<EventEntity> events = eventRepository.findByAggregateId(auction.getId());
            // for (EventEntity event : events) {
            //     ArchivedEvent archivedEvent = mapToArchivedEvent(event);
            //     archivedEventRepository.save(archivedEvent);
            // }

            // Delete from main tables
            eventRepository.deleteByAggregateId(auction.getId());
            bidRepository.deleteByAuctionId(auction.getId());
            auctionRepository.delete(auction);
        }
    }

    private ArchivedAuction mapToArchivedAuction(Auction auction) {
        ArchivedAuction archived = new ArchivedAuction();
        archived.setId(auction.getId());
        archived.setItemId(auction.getItemId());
        archived.setStatus(auction.getStatus());
        archived.setStartTs(auction.getStartTs());
        archived.setEndTs(auction.getEndTs());
        archived.setEncryptedReservePrice(auction.getEncryptedReservePrice());
        archived.setBuyNowPrice(auction.getBuyNowPrice());
        archived.setHiddenReserve(auction.isHiddenReserve());
        archived.setArchivedAt(Instant.now());
        return archived;
    }

    private ArchivedBid mapToArchivedBid(Bid bid) {
        ArchivedBid archived = new ArchivedBid();
        archived.setAuctionId(bid.getAuctionId());
        archived.setBidderId(bid.getBidderId());
        archived.setAmount(bid.getAmount());
        archived.setServerTs(bid.getServerTs());
        archived.setSeqNo(bid.getSeqNo());
        archived.setAccepted(bid.getAccepted());
        archived.setArchivedAt(Instant.now());
        return archived;
    }

    private ArchivedEvent mapToArchivedEvent(EventEntity event) {
        ArchivedEvent archived = new ArchivedEvent();
        archived.setAggregateId(event.getAggregateId());
        archived.setAggregateType(event.getAggregateType());
        archived.setEventType(event.getEventType());
        try {
            archived.setCompressedEventData(compress(event.getEventData()));
            if (event.getEventMetadata() != null) {
                archived.setCompressedEventMetadata(compress(event.getEventMetadata()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress event data", e);
        }
        archived.setSequenceNumber(event.getSequenceNumber());
        archived.setTimestamp(event.getTimestamp());
        archived.setArchivedAt(Instant.now());
        return archived;
    }

    private byte[] compress(String data) throws IOException {
        if (data == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data.getBytes("UTF-8"));
        }
        return baos.toByteArray();
    }
}