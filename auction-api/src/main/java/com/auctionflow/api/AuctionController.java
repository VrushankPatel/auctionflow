package com.auctionflow.api;

import com.auctionflow.api.dtos.*;
import com.auctionflow.api.queryhandlers.GetAuctionDetailsQueryHandler;
import com.auctionflow.api.queryhandlers.GetBidHistoryQueryHandler;
import com.auctionflow.api.queryhandlers.ListActiveAuctionsQueryHandler;
import com.auctionflow.api.queries.GetAuctionDetailsQuery;
import com.auctionflow.api.queries.GetBidHistoryQuery;
import com.auctionflow.api.queries.ListActiveAuctionsQuery;
import com.auctionflow.core.domain.commands.*;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.ItemId;
import com.auctionflow.core.domain.valueobjects.Money;
import com.auctionflow.events.command.CommandBus;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auctions")
@Tag(name = "Auctions", description = "Auction management endpoints")
public class AuctionController {

    private final CommandBus commandBus;
    private final ListActiveAuctionsQueryHandler listHandler;
    private final GetAuctionDetailsQueryHandler detailsHandler;
    private final GetBidHistoryQueryHandler bidHistoryHandler;

    public AuctionController(CommandBus commandBus,
                            ListActiveAuctionsQueryHandler listHandler,
                            GetAuctionDetailsQueryHandler detailsHandler,
                            GetBidHistoryQueryHandler bidHistoryHandler) {
        this.commandBus = commandBus;
        this.listHandler = listHandler;
        this.detailsHandler = detailsHandler;
        this.bidHistoryHandler = bidHistoryHandler;
    }

    @PostMapping
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
                examples = @ExampleObject(
                    value = """
                    {
                      "itemId": "123e4567-e89b-12d3-a456-426614174000",
                      "reservePrice": 100.00,
                      "buyNowPrice": 500.00,
                      "startTime": "2023-10-01T10:00:00Z",
                      "endTime": "2023-10-01T12:00:00Z"
                    }
                    """
                )
            )
        )
        @Valid @RequestBody CreateAuctionRequest request) {
        ItemId itemId = new ItemId(request.getItemId());
        Money reservePrice = new Money(request.getReservePrice());
        Money buyNowPrice = new Money(request.getBuyNowPrice());
        CreateAuctionCommand cmd = new CreateAuctionCommand(itemId, reservePrice, buyNowPrice, request.getStartTime(), request.getEndTime());
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(
        summary = "List active auctions",
        description = "Retrieves a paginated list of active auctions, optionally filtered by category and seller."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of auctions retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ActiveAuctionsDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content)
    })
    public ResponseEntity<ActiveAuctionsDTO> listAuctions(
            @RequestParam Optional<String> category,
            @RequestParam Optional<String> sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ListActiveAuctionsQuery query = new ListActiveAuctionsQuery(category, sellerId, page, size);
        ActiveAuctionsDTO dto = listHandler.handle(query);
        return ResponseEntity.ok(dto);
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
    public ResponseEntity<AuctionDetailsDTO> getAuction(@PathVariable String id) {
        GetAuctionDetailsQuery query = new GetAuctionDetailsQuery(id);
        Optional<AuctionDetailsDTO> dto = detailsHandler.handle(query);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/bids")
    @Operation(
        summary = "Place a bid on an auction",
        description = "Places a bid on the specified auction. Bids are processed with server timestamp and sequence number for fairness."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bid placed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid bid amount or auction not active", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> placeBid(
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
        AuctionId auctionId = new AuctionId(id);
        UUID bidderId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String clientIp = getClientIp(httpRequest);
        Money amount = new Money(request.getAmount());
        String idempotencyKey = request.getIdempotencyKey();

        // Check rate limits
        RateLimiter perUserLimiter = rateLimiterRegistry.rateLimiter("perUser");
        RateLimiter perIpLimiter = rateLimiterRegistry.rateLimiter("perIp");
        RateLimiter perAuctionLimiter = rateLimiterRegistry.rateLimiter("perAuction");

        if (!perUserLimiter.acquirePermission(bidderId.toString())) {
            addRateLimitHeaders(response, perUserLimiter, bidderId.toString());
            return ResponseEntity.status(429).build();
        }
        if (!perIpLimiter.acquirePermission(clientIp)) {
            addRateLimitHeaders(response, perIpLimiter, clientIp);
            return ResponseEntity.status(429).build();
        }
        if (!perAuctionLimiter.acquirePermission(id)) {
            addRateLimitHeaders(response, perAuctionLimiter, id);
            return ResponseEntity.status(429).build();
        }

        // Add headers for successful requests
        addRateLimitHeaders(response, perUserLimiter, bidderId.toString());
        addRateLimitHeaders(response, perIpLimiter, clientIp);
        addRateLimitHeaders(response, perAuctionLimiter, id);

        PlaceBidCommand cmd = new PlaceBidCommand(auctionId, bidderId, amount, idempotencyKey);
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
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/buy-now")
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
        AuctionId auctionId = new AuctionId(id);
        UUID buyerId = UUID.fromString(request.getUserId());
        BuyNowCommand cmd = new BuyNowCommand(auctionId, buyerId);
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/watch")
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
        AuctionId auctionId = new AuctionId(id);
        UUID userId = UUID.fromString(request.getUserId());
        WatchCommand cmd = new WatchCommand(auctionId, userId);
        commandBus.send(cmd);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/watch")
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
        AuctionId auctionId = new AuctionId(id);
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

    private void addRateLimitHeaders(HttpServletResponse response, RateLimiter limiter, String key) {
        long available = limiter.getMetrics().getAvailablePermissions();
        long limit = limiter.getRateLimiterConfig().getLimitForPeriod();
        response.addHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.addHeader("X-RateLimit-Remaining", String.valueOf(available));
        // Approximate reset time
        long reset = System.currentTimeMillis() / 1000 + limiter.getRateLimiterConfig().getLimitRefreshPeriod().getSeconds();
        response.addHeader("X-RateLimit-Reset", String.valueOf(reset));
    }
}