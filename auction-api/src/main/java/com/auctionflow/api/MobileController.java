package com.auctionflow.api;

import com.auctionflow.api.dtos.*;
import com.auctionflow.api.queryhandlers.ListActiveAuctionsQueryHandler;
import com.auctionflow.api.queries.ListActiveAuctionsQuery;
import com.auctionflow.api.services.MobileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mobile")
@Tag(name = "Mobile", description = "Mobile-specific endpoints for optimized payloads, offline support, and device management")
public class MobileController {

    private final ListActiveAuctionsQueryHandler listHandler;
    private final MobileService mobileService;

    public MobileController(ListActiveAuctionsQueryHandler listHandler, MobileService mobileService) {
        this.listHandler = listHandler;
        this.mobileService = mobileService;
    }

    @GetMapping("/auctions")
    @Operation(
        summary = "List auctions with optimized payloads for mobile",
        description = "Retrieves a paginated list of active auctions with reduced fields for mobile clients."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of auctions retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MobileAuctionsDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content)
    })
    public ResponseEntity<MobileAuctionsDTO> listMobileAuctions(
            @RequestParam Optional<String> category,
            @RequestParam Optional<Long> sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ListActiveAuctionsQuery query = new ListActiveAuctionsQuery(category, sellerId, Optional.empty(), page, size);
        ActiveAuctionsDTO fullDto = listHandler.handle(query);
        // Convert to mobile optimized DTO
        MobileAuctionsDTO mobileDto = mobileService.convertToMobileAuctions(fullDto);
        return ResponseEntity.ok().header("Cache-Control", "max-age=30").body(mobileDto);
    }

    @PostMapping(value = "/images/compress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Compress image for mobile upload",
        description = "Uploads an image and returns a compressed version optimized for mobile."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Compressed image returned",
            content = @Content(mediaType = "application/octet-stream")),
        @ApiResponse(responseCode = "400", description = "Invalid image", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> compressImage(@RequestParam("image") MultipartFile image) {
        try {
            byte[] compressed = mobileService.compressImage(image.getBytes());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("Content-Disposition", "attachment; filename=\"compressed.jpg\"")
                    .body(compressed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/sync")
    @Operation(
        summary = "Sync offline data",
        description = "Syncs pending bids and actions from offline mobile client."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sync completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid sync data", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> syncOfflineData(@Valid @RequestBody OfflineSyncRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        mobileService.syncOfflineData(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notifications/register")
    @Operation(
        summary = "Register device for push notifications",
        description = "Registers a device token for receiving push notifications."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Device registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> registerPushNotification(@Valid @RequestBody PushNotificationRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        mobileService.registerPushToken(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/devices/track")
    @Operation(
        summary = "Track device information",
        description = "Tracks device information for analytics and security."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Device tracked successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> trackDevice(@Valid @RequestBody DeviceTrackingRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        mobileService.trackDevice(userId, request);
        return ResponseEntity.ok().build();
    }
}