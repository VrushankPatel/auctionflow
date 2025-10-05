package com.auctionflow.api.services;

import com.auctionflow.api.dtos.*;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MobileService {

    public MobileAuctionsDTO convertToMobileAuctions(ActiveAuctionsDTO fullDto) {
        List<MobileAuctionDTO> mobileAuctions = fullDto.getAuctions().stream()
                .map(auction -> new MobileAuctionDTO(
                        auction.getId(),
                        auction.getTitle(),
                        auction.getCurrentHighestBid(),
                        auction.getEndTime(),
                        auction.getImages() != null && !auction.getImages().isEmpty() ? auction.getImages().get(0) : null
                ))
                .collect(Collectors.toList());
        return new MobileAuctionsDTO(mobileAuctions, fullDto.getPage(), fullDto.getSize(), fullDto.getTotalElements(), fullDto.getTotalPages());
    }

    public byte[] compressImage(byte[] imageBytes) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(new java.io.ByteArrayInputStream(imageBytes))
                .size(800, 600) // Resize to max 800x600
                .outputQuality(0.8) // 80% quality
                .outputFormat("jpg")
                .toOutputStream(outputStream);
        return outputStream.toByteArray();
    }

    public void syncOfflineData(UUID userId, OfflineSyncRequest request) {
        // Process pending bids: place them via command bus
        // For now, just log or store; in real impl, send PlaceBidCommand for each
        for (OfflineSyncRequest.PendingBid pending : request.getPendingBids()) {
            // commandBus.send(new PlaceBidCommand(...));
            System.out.println("Syncing bid for user " + userId + " on auction " + pending.getAuctionId());
        }
    }

    public void registerPushToken(UUID userId, PushNotificationRequest request) {
        // Store in DB: userId, deviceToken, deviceId, platform
        // Assume a repository for device tokens
        System.out.println("Registered push token for user " + userId + ": " + request.getDeviceToken());
    }

    public void trackDevice(UUID userId, DeviceTrackingRequest request) {
        // Store device info for analytics
        System.out.println("Tracked device for user " + userId + ": " + request.getDeviceModel());
    }
}