package com.auctionflow.api;

import com.auctionflow.api.dtos.*;
import com.auctionflow.api.entities.Item;
import java.time.Instant;
import com.auctionflow.api.entities.User;
import com.auctionflow.api.queryhandlers.GetAuctionDetailsQueryHandler;
import com.auctionflow.api.queryhandlers.GetBidHistoryQueryHandler;
import com.auctionflow.api.queryhandlers.ListActiveAuctionsQueryHandler;
import com.auctionflow.api.queries.GetAuctionDetailsQuery;
import com.auctionflow.api.queries.GetBidHistoryQuery;
import com.auctionflow.api.queries.ListActiveAuctionsQuery;
import com.auctionflow.api.repositories.ItemRepository;
import com.auctionflow.api.services.ItemValidationService;
import com.auctionflow.api.services.ProxyBidService;
import com.auctionflow.api.services.SuspiciousActivityService;
import com.auctionflow.api.services.UserService;
import com.auctionflow.common.service.FeatureFlagService;
import com.auctionflow.common.service.SequenceService;
import com.auctionflow.core.domain.commands.*;
import com.auctionflow.core.domain.commands.MakeOfferCommand;
import com.auctionflow.core.domain.valueobjects.AntiSnipePolicy;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.ItemId;
import com.auctionflow.core.domain.valueobjects.Money;
import com.auctionflow.core.domain.valueobjects.SellerId;
import com.auctionflow.events.command.CommandBus;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
// import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.getunleash.UnleashContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
// import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auctions")
@Tag(name = "Auctions", description = "Auction management endpoints")
public class AuctionController {

    private final SequenceService sequenceService;
    private final CommandBus commandBus;
    private final ListActiveAuctionsQueryHandler listHandler;
    private final GetAuctionDetailsQueryHandler detailsHandler;
    private final GetBidHistoryQueryHandler bidHistoryHandler;
    private final SuspiciousActivityService suspiciousActivityService;
    private final ProxyBidService proxyBidService;
    private final UserService userService;
    private final ItemValidationService itemValidationService;
    private final ItemRepository itemRepository;
    private final FeatureFlagService featureFlagService;
    private final RateLimiterRegistry rateLimiterRegistry;

    public AuctionController(SequenceService sequenceService,
                              CommandBus commandBus,
                              ListActiveAuctionsQueryHandler listHandler,
                              GetAuctionDetailsQueryHandler detailsHandler,
                              GetBidHistoryQueryHandler bidHistoryHandler,
                              SuspiciousActivityService suspiciousActivityService,
                              ProxyBidService proxyBidService,
                              UserService userService,
                              ItemValidationService itemValidationService,
                               ItemRepository itemRepository,
                               FeatureFlagService featureFlagService,
                               RateLimiterRegistry rateLimiterRegistry) {
        this.sequenceService = sequenceService;
        this.commandBus = commandBus;
        this.listHandler = listHandler;
        this.detailsHandler = detailsHandler;
        this.bidHistoryHandler = bidHistoryHandler;
        this.suspiciousActivityService = suspiciousActivityService;
        this.proxyBidService = proxyBidService;
        this.userService = userService;
        this.itemValidationService = itemValidationService;
        this.itemRepository = itemRepository;
        this.featureFlagService = featureFlagService;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @Operation(
        summary = "Create a new auction",
        description = "Creates a new auction for an item with specified reserve price, buy-now price, start and end times."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Auction created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> createAuction(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Auction creation request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateAuctionRequest.class),
                  examples = {
                       @ExampleObject(
                           name = "Basic Auction",
                           value = """
                           {
                             "itemId": "123e4567-e89b-12d3-a456-426614174000",
                             "categoryId": "electronics",
                             "auctionType": "ENGLISH_OPEN",
                             "reservePrice": 100.00,
                             "buyNowPrice": 500.00,
                             "startTime": "2023-10-01T10:00:00Z",
                             "endTime": "2023-10-01T12:00:00Z"
                           }
                           """
                       ),
                       @ExampleObject(
                           name = "Reserve Auction",
                           value = """
                           {
                             "itemId": "123e4567-e89b-12d3-a456-426614174000",
                             "categoryId": "art",
                             "auctionType": "ENGLISH_OPEN",
                             "reservePrice": 500.00,
                             "buyNowPrice": null,
                             "startTime": "2023-10-01T10:00:00Z",
                             "endTime": "2023-10-01T12:00:00Z",
                             "hiddenReserve": true
                           }
                           """
                       )
                   }
            )
        )
        @Valid @RequestBody CreateAuctionRequest request) {
        // Check seller verification
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByEmail(email);
        if (user == null || !userService.isKycVerified(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // Or bad request with message
        }

        // Get item and validate
        Item item = itemRepository.findById(request.getItemId()).orElse(null);
        if (item == null) {
            return ResponseEntity.badRequest().build();
        }
        ItemValidationService.ValidationResult validation = itemValidationService.validateItem(item.getCategoryId(), item.getTitle(), item.getDescription(), item.getBrand(), item.getSerialNumber());
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().build(); // Or return reason
        }

        ItemId itemId = new ItemId(UUID.fromString(request.getItemId()));
        SellerId sellerId = new SellerId(UUID.fromString(user.getId().toString()));
        Money reservePrice = Money.usd(request.getReservePrice());
        Money buyNowPrice = Money.usd(request.getBuyNowPrice());
        CreateAuctionCommand cmd = new CreateAuctionCommand(itemId, sellerId, request.getCategoryId(), request.getAuctionType(), reservePrice, buyNowPrice, request.getStartTime(), request.getEndTime(), AntiSnipePolicy.none(), request.isHiddenReserve()); // Assuming default policy
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(
        summary = "List active auctions",
        description = "Retrieves a paginated list of active auctions, optionally filtered by category, seller, or search query."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of auctions retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ActiveAuctionsDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content)
    })
    public ResponseEntity<ActiveAuctionsDTO> listAuctions(
            @RequestParam Optional<String> category,
            @RequestParam Optional<String> sellerId,
            @RequestParam Optional<String> query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ListActiveAuctionsQuery listQuery = new ListActiveAuctionsQuery(category, sellerId, query, page, size);
        ActiveAuctionsDTO dto = listHandler.handle(listQuery);
        return ResponseEntity.ok().header("Cache-Control", "max-age=30").body(dto);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@auctionSecurityService.canEdit(#id, authentication.principal) or hasRole('ADMIN')")
    @Operation(
        summary = "Update auction metadata",
        description = "Updates auction metadata before the first bid or by admin."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Auction updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> updateAuction(@PathVariable String id, @RequestBody UpdateAuctionRequest request) {
        // Assume UpdateAuctionCommand exists
        UpdateAuctionCommand cmd = new UpdateAuctionCommand(new AuctionId(UUID.fromString(id)), request.getTitle(), request.getDescription());
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get auction details",
        description = "Retrieves detailed information about a specific auction including current highest bid."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Auction details retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuctionDetailsDTO.class))),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content)
    })
    @org.springframework.cache.annotation.Cacheable(value = "auctionDetails", key = "#id")
    public ResponseEntity<AuctionDetailsDTO> getAuction(@PathVariable String id) {
        GetAuctionDetailsQuery query = new GetAuctionDetailsQuery(id);
        Optional<AuctionDetailsDTO> dto = detailsHandler.handle(query);

        // A/B testing: get UI variant
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UnleashContext context = UnleashContext.builder()
                .userId(userId)
                .build();
        String uiVariant = featureFlagService.getVariant("auction_ui_experiment", context);

        return dto.map(d -> ResponseEntity.ok()
                .header("Cache-Control", "max-age=60")
                .header("X-UI-Variant", uiVariant)
                .body(d))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/bids")
    @PreAuthorize("@auctionSecurityService.canBid(#id, authentication.principal)")
    // @WithSpan("place-bid")
    @Operation(
        summary = "Place a bid on an auction",
        description = "Places a bid on the specified auction. Bids are processed with server timestamp and sequence number for fairness."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bid placed successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BidResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid bid amount or auction not active", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @org.springframework.cache.annotation.CacheEvict(value = "auctionDetails", key = "#id")
    public ResponseEntity<BidResponse> placeBid(
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Bid placement request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PlaceBidRequest.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "amount": 150.00,
                      "idempotencyKey": "unique-key-123"
                    }
                    """
                )
            )
        )
        @Valid @RequestBody PlaceBidRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse response) {
        AuctionId auctionId = new AuctionId(UUID.fromString(id));
        UUID bidderId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        Money amount = Money.usd(request.getAmount());
        String idempotencyKey = request.getIdempotencyKey();

        // Kill switch: check if bidding is globally disabled
        if (featureFlagService.isEnabled("bidding_kill_switch")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build(); // Bidding temporarily disabled
        }

        // Feature flag check for advanced bidding features
        UnleashContext context = UnleashContext.builder()
                .userId(bidderId.toString())
                .build();
        if (!featureFlagService.isEnabled("advanced_bidding", context)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // Feature not enabled for this user
        }

        // Check for suspicious activity
        suspiciousActivityService.checkForSuspiciousActivity(bidderId.toString(), clientIp, userAgent, "BID_PLACEMENT");

        // Check rate limits
        io.github.resilience4j.ratelimiter.RateLimiter perUserLimiter = rateLimiterRegistry.rateLimiter("perUser");
        io.github.resilience4j.ratelimiter.RateLimiter perIpLimiter = rateLimiterRegistry.rateLimiter("perIp");
        io.github.resilience4j.ratelimiter.RateLimiter perAuctionLimiter = rateLimiterRegistry.rateLimiter("perAuction");

        if (!perUserLimiter.acquirePermission(bidderId.toString().hashCode())) {
            addRateLimitHeaders(response, perUserLimiter, bidderId.toString());
            return ResponseEntity.status(429).build();
        }
        if (!perIpLimiter.acquirePermission(clientIp.hashCode())) {
            addRateLimitHeaders(response, perIpLimiter, clientIp);
            return ResponseEntity.status(429).build();
        }
        if (!perAuctionLimiter.acquirePermission(id.hashCode())) {
            addRateLimitHeaders(response, perAuctionLimiter, id);
            return ResponseEntity.status(429).build();
        }

        // Add headers for successful requests
        addRateLimitHeaders(response, perUserLimiter, bidderId.toString());
        addRateLimitHeaders(response, perIpLimiter, clientIp);
        addRateLimitHeaders(response, perAuctionLimiter, id);

        Instant serverTs = Instant.now();
        long seqNo = sequenceService.nextSequence(auctionId);

        // For asynchronous processing, return immediately with optimistic acceptance
        // The actual validation and processing happens asynchronously
        BidResponse bidResponse = new BidResponse();
        bidResponse.setAccepted(true); // Optimistically accept
        bidResponse.setServerTimestamp(serverTs);
        bidResponse.setSequenceNumber(seqNo);

        // Send command asynchronously
        PlaceBidCommand cmd = new PlaceBidCommand(auctionId, bidderId, amount, idempotencyKey, serverTs, seqNo);
        // Use async executor for command processing
        commandBus.sendAsync(cmd);

        return ResponseEntity.ok(bidResponse);
    }

    @PostMapping("/{id}/proxy-bid")
    @PreAuthorize("@auctionSecurityService.canBid(#id, authentication.principal)")
    @Operation(
        summary = "Set a proxy bid for an auction",
        description = "Sets a maximum bid amount for automatic bidding on the specified auction."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Proxy bid set successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or auction not active", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> setProxyBid(
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Proxy bid request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PlaceBidRequest.class), // Reuse for simplicity
                examples = @ExampleObject(
                    value = """
                    {
                      "amount": 200.00
                    }
                    """
                )
            )
        )
        @Valid @RequestBody PlaceBidRequest request) {
        AuctionId auctionId = new AuctionId(UUID.fromString(id));
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Money maxBid = Money.usd(request.getAmount());
        proxyBidService.setProxyBid(userId, auctionId, maxBid, null, null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/commits")
    @PreAuthorize("@auctionSecurityService.canBid(#id, authentication.principal)")
    // @WithSpan("commit-bid")
    @Operation(
        summary = "Commit a bid hash for sealed auction",
        description = "Commits a bid hash for a sealed-bid auction during the bidding phase."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bid committed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or auction not in bidding phase", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> commitBid(
        @PathVariable String id,
        @Valid @RequestBody CommitBidRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse response) {
        AuctionId auctionId = new AuctionId(UUID.fromString(id));
        UUID bidderId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Check for suspicious activity
        suspiciousActivityService.checkForSuspiciousActivity(bidderId.toString(), clientIp, userAgent, "BID_COMMIT");

        // Rate limits similar to placeBid
        io.github.resilience4j.ratelimiter.RateLimiter perUserLimiter = rateLimiterRegistry.rateLimiter("perUser");
        io.github.resilience4j.ratelimiter.RateLimiter perIpLimiter = rateLimiterRegistry.rateLimiter("perIp");
        io.github.resilience4j.ratelimiter.RateLimiter perAuctionLimiter = rateLimiterRegistry.rateLimiter("perAuction");

        if (!perUserLimiter.acquirePermission(bidderId.toString().hashCode())) {
            addRateLimitHeaders(response, perUserLimiter, bidderId.toString());
            return ResponseEntity.status(429).build();
        }
        if (!perIpLimiter.acquirePermission(clientIp.hashCode())) {
            addRateLimitHeaders(response, perIpLimiter, clientIp);
            return ResponseEntity.status(429).build();
        }
        if (!perAuctionLimiter.acquirePermission(id.hashCode())) {
            addRateLimitHeaders(response, perAuctionLimiter, id);
            return ResponseEntity.status(429).build();
        }

        addRateLimitHeaders(response, perUserLimiter, bidderId.toString());
        addRateLimitHeaders(response, perIpLimiter, clientIp);
        addRateLimitHeaders(response, perAuctionLimiter, id);

        CommitBidCommand cmd = new CommitBidCommand(auctionId, new BidderId(bidderId), request.getBidHash(), request.getSalt());
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reveals")
    @PreAuthorize("@auctionSecurityService.canBid(#id, authentication.principal)")
    // @WithSpan("reveal-bid")
    @Operation(
        summary = "Reveal a bid for sealed auction",
        description = "Reveals the actual bid amount for a sealed-bid auction during the reveal phase."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bid revealed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid reveal or auction not in reveal phase", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> revealBid(
        @PathVariable String id,
        @Valid @RequestBody RevealBidRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse response) {
        AuctionId auctionId = new AuctionId(UUID.fromString(id));
        UUID bidderId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Check for suspicious activity
        suspiciousActivityService.checkForSuspiciousActivity(bidderId.toString(), clientIp, userAgent, "BID_REVEAL");

        // Rate limits
        io.github.resilience4j.ratelimiter.RateLimiter perUserLimiter = rateLimiterRegistry.rateLimiter("perUser");
        io.github.resilience4j.ratelimiter.RateLimiter perIpLimiter = rateLimiterRegistry.rateLimiter("perIp");
        io.github.resilience4j.ratelimiter.RateLimiter perAuctionLimiter = rateLimiterRegistry.rateLimiter("perAuction");

        if (!perUserLimiter.acquirePermission(bidderId.toString().hashCode())) {
            addRateLimitHeaders(response, perUserLimiter, bidderId.toString());
            return ResponseEntity.status(429).build();
        }
        if (!perIpLimiter.acquirePermission(clientIp.hashCode())) {
            addRateLimitHeaders(response, perIpLimiter, clientIp);
            return ResponseEntity.status(429).build();
        }
        if (!perAuctionLimiter.acquirePermission(id.hashCode())) {
            addRateLimitHeaders(response, perAuctionLimiter, id);
            return ResponseEntity.status(429).build();
        }

        addRateLimitHeaders(response, perUserLimiter, bidderId.toString());
        addRateLimitHeaders(response, perIpLimiter, clientIp);
        addRateLimitHeaders(response, perAuctionLimiter, id);

        Money amount = Money.usd(request.getAmount());
        RevealBidCommand cmd = new RevealBidCommand(auctionId, new BidderId(bidderId), amount, request.getSalt());
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/bids")
    @Operation(
        summary = "Get bid history for an auction",
        description = "Retrieves the paginated bid history for a specific auction, including accepted and rejected bids."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bid history retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BidHistoryDTO.class))),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content)
    })
    public ResponseEntity<BidHistoryDTO> getBidHistory(@PathVariable String id,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        GetBidHistoryQuery query = new GetBidHistoryQuery(id, page, size);
        BidHistoryDTO dto = bidHistoryHandler.handle(query);
        return ResponseEntity.ok().header("Cache-Control", "no-cache").body(dto);
    }

    @PostMapping("/{id}/buy-now")
    @PreAuthorize("@auctionSecurityService.canBid(#id, authentication.principal)")
    @Operation(
        summary = "Buy auction immediately",
        description = "Executes an immediate purchase of the auction at the buy-now price."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Buy-now executed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or auction not eligible for buy-now", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> buyNow(
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Buy-now request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BuyNowRequest.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "userId": "123e4567-e89b-12d3-a456-426614174000"
                    }
                    """
                )
            )
        )
        @Valid @RequestBody BuyNowRequest request) {
        AuctionId auctionId = new AuctionId(UUID.fromString(id));
        UUID buyerId = UUID.fromString(request.getUserId());
        BuyNowCommand cmd = new BuyNowCommand(auctionId, buyerId);
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/offers")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Make an offer on an auction",
        description = "Submits an offer for the auction item."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Offer made successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid offer", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> makeOffer(
        @PathVariable String id,
        @Valid @RequestBody MakeOfferRequest request) {
        AuctionId auctionId = new AuctionId(UUID.fromString(id));
        UUID buyerId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // Get sellerId from auction aggregate
        GetAuctionDetailsQuery query = new GetAuctionDetailsQuery(id);
        Optional<AuctionDetailsDTO> dtoOpt = detailsHandler.handle(query);
        if (dtoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // Assume AuctionDetailsDTO has sellerId, but it may not. For now, get from item
        Item item = itemRepository.findById(dtoOpt.get().getItemId()).orElse(null);
        if (item == null) {
            return ResponseEntity.badRequest().build();
        }
        SellerId sellerId = new SellerId(UUID.fromString(item.getSellerId()));
        Money amount = Money.usd(request.getAmount());
        MakeOfferCommand cmd = new MakeOfferCommand(auctionId, new BidderId(buyerId), sellerId, amount);
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/watch")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Add auction to watchlist",
        description = "Adds the specified auction to the user's watchlist for notifications."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Auction added to watchlist successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> watch(
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Watch request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WatchRequest.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "userId": "123e4567-e89b-12d3-a456-426614174000"
                    }
                    """
                )
            )
        )
        @Valid @RequestBody WatchRequest request) {
        AuctionId auctionId = new AuctionId(UUID.fromString(id));
        UUID userId = UUID.fromString(request.getUserId());
        WatchCommand cmd = new WatchCommand(auctionId, userId);
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Force close auction",
        description = "Admin forces the closure of an auction."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Auction closed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> closeAuction(@PathVariable String id) {
        CloseAuctionCommand cmd = new CloseAuctionCommand(new AuctionId(UUID.fromString(id)));
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/watch")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Remove auction from watchlist",
        description = "Removes the specified auction from the user's watchlist."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Auction removed from watchlist successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> unwatch(
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Unwatch request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WatchRequest.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "userId": "123e4567-e89b-12d3-a456-426614174000"
                    }
                    """
                )
            )
        )
        @Valid @RequestBody WatchRequest request) {
        AuctionId auctionId = new AuctionId(UUID.fromString(id));
        UUID userId = UUID.fromString(request.getUserId());
        UnwatchCommand cmd = new UnwatchCommand(auctionId, userId);
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }



    @GetMapping("/{id}/offers")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "List offers for auction",
        description = "Lists all offers made on the auction (for seller)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of offers",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OfferResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<OfferResponse>> getOffers(@PathVariable String id) {
        // Assume GetOffersQuery and handler exist, or implement simple query
        // For now, return empty list as placeholder
        List<OfferResponse> offers = new ArrayList<>();
        return ResponseEntity.ok(offers);
    }

    private void addRateLimitHeaders(HttpServletResponse response, io.github.resilience4j.ratelimiter.RateLimiter limiter, String key) {
        long available = limiter.getMetrics().getAvailablePermissions();
        long limit = limiter.getRateLimiterConfig().getLimitForPeriod();
        response.addHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.addHeader("X-RateLimit-Remaining", String.valueOf(available));
        // Approximate reset time
        long reset = System.currentTimeMillis() / 1000 + limiter.getRateLimiterConfig().getLimitRefreshPeriod().getSeconds();
        response.addHeader("X-RateLimit-Reset", String.valueOf(reset));
    }
}