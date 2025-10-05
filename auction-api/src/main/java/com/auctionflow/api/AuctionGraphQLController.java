package com.auctionflow.api;

import com.auctionflow.api.dtos.ActiveAuctionsDTO;
import com.auctionflow.api.dtos.AuctionDetailsDTO;
import com.auctionflow.api.dtos.BidHistoryDTO;
import com.auctionflow.api.entities.Item;
import com.auctionflow.api.entities.User;
import com.auctionflow.api.queryhandlers.GetAuctionDetailsQueryHandler;
import com.auctionflow.api.queryhandlers.GetBidHistoryQueryHandler;
import com.auctionflow.api.queryhandlers.ListActiveAuctionsQueryHandler;
import com.auctionflow.api.queries.GetAuctionDetailsQuery;
import com.auctionflow.api.queries.GetBidHistoryQuery;
import com.auctionflow.api.queries.ListActiveAuctionsQuery;
import com.auctionflow.api.repositories.ItemRepository;
import com.auctionflow.api.repositories.UserRepository;
import com.auctionflow.api.services.ItemValidationService;
import java.util.Currency;
import java.time.Instant;
import java.time.ZoneOffset;
import com.auctionflow.api.services.UserService;
import com.auctionflow.core.domain.commands.*;
import com.auctionflow.core.domain.valueobjects.AntiSnipePolicy;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.AuctionType;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;
import com.auctionflow.events.command.CommandBus;
import com.auctionflow.common.service.SequenceService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Controller
@Profile("!ui-only")
public class AuctionGraphQLController {

    private final CommandBus commandBus;
    private final ListActiveAuctionsQueryHandler listHandler;
    private final GetAuctionDetailsQueryHandler detailsHandler;
    private final GetBidHistoryQueryHandler bidHistoryHandler;
    private final UserService userService;
    private final ItemValidationService itemValidationService;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final SequenceService sequenceService;

    public AuctionGraphQLController(CommandBus commandBus,
                                     ListActiveAuctionsQueryHandler listHandler,
                                     GetAuctionDetailsQueryHandler detailsHandler,
                                     GetBidHistoryQueryHandler bidHistoryHandler,
                                     UserService userService,
                                     ItemValidationService itemValidationService,
                                     ItemRepository itemRepository,
                                     UserRepository userRepository,
                                     SequenceService sequenceService) {
        this.commandBus = commandBus;
        this.listHandler = listHandler;
        this.detailsHandler = detailsHandler;
        this.bidHistoryHandler = bidHistoryHandler;
        this.userService = userService;
        this.itemValidationService = itemValidationService;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.sequenceService = sequenceService;
    }

    @QueryMapping
    public ActiveAuctionsDTO auctions(@Argument String category, @Argument String sellerId, @Argument Integer page, @Argument Integer size) {
        ListActiveAuctionsQuery query = new ListActiveAuctionsQuery(Optional.ofNullable(category), sellerId != null ? Optional.of(Long.parseLong(sellerId)) : Optional.empty(), Optional.empty(), page != null ? page : 0, size != null ? size : 10);
        return listHandler.handle(query);
    }

    @QueryMapping
    public AuctionDetailsDTO auction(@Argument String id) {
        GetAuctionDetailsQuery query = new GetAuctionDetailsQuery(id);
        return detailsHandler.handle(query).orElse(null);
    }

    @QueryMapping
    public BidHistoryDTO bids(@Argument String auctionId, @Argument Integer page, @Argument Integer size) {
        GetBidHistoryQuery query = new GetBidHistoryQuery(auctionId, page != null ? page : 0, size != null ? size : 10);
        return bidHistoryHandler.handle(query);
    }

    @QueryMapping
    public User user(@Argument String id) {
        return userRepository.findById(Long.valueOf(id)).orElse(null);
    }

    @MutationMapping
    public AuctionDetailsDTO createAuction(@Argument CreateAuctionInput input) {
        // Similar to REST controller
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByEmail(email);
        if (user == null) { // TODO: Fix KYC verification - || !userService.isKycVerified(user.getId())
            throw new RuntimeException("Unauthorized");
        }

        Item item = itemRepository.findById(input.getItemId()).orElse(null);
        if (item == null) {
            throw new RuntimeException("Item not found");
        }
        ItemValidationService.ValidationResult validation = itemValidationService.validateItem(item.getCategoryId(), item.getTitle(), item.getDescription(), item.getBrand(), item.getSerialNumber());
        if (!validation.isValid()) {
            throw new RuntimeException("Invalid item");
        }

        AuctionId auctionId = AuctionId.generate();
        Money reservePrice = input.getReservePrice() != null ? Money.usd(BigDecimal.valueOf(input.getReservePrice())) : null;
        Money buyNowPrice = input.getBuyNowPrice() != null ? Money.usd(BigDecimal.valueOf(input.getBuyNowPrice())) : null;
        CreateAuctionCommand cmd = new CreateAuctionCommand(
            auctionId,
            new com.auctionflow.core.domain.valueobjects.ItemId(input.getItemId()),
            new com.auctionflow.core.domain.valueobjects.SellerId(user.getId().toString()),
            input.getCategoryId(),
            AuctionType.valueOf(input.getAuctionType()),
            reservePrice,
            buyNowPrice,
            input.getStartTime().toInstant(ZoneOffset.UTC),
            input.getEndTime().toInstant(ZoneOffset.UTC),
            AntiSnipePolicy.none(),
            input.isHiddenReserve()
        );
        commandBus.send(cmd);

        // Return the created auction details
        GetAuctionDetailsQuery query = new GetAuctionDetailsQuery(auctionId.value().toString());
        return detailsHandler.handle(query).orElse(null);
    }

    @MutationMapping
    public AuctionDetailsDTO placeBid(@Argument String auctionId, @Argument PlaceBidInput input) {
        UUID bidderId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Money amount = Money.usd(BigDecimal.valueOf(input.getAmount()));
        Instant serverTs = Instant.now();
        long seqNo = sequenceService.nextSequence(new AuctionId(auctionId));
        PlaceBidCommand cmd = new PlaceBidCommand(new AuctionId(auctionId), bidderId.toString(), amount, input.getIdempotencyKey(), serverTs, seqNo);
        commandBus.send(cmd);
        // Return updated auction
        GetAuctionDetailsQuery query = new GetAuctionDetailsQuery(auctionId);
        return detailsHandler.handle(query).orElse(null);
    }

    @MutationMapping
    public Boolean watchAuction(@Argument String auctionId, @Argument String userId) {
        WatchCommand cmd = new WatchCommand(new AuctionId(auctionId), userId);
        commandBus.send(cmd);
        return true;
    }

    @MutationMapping
    public Boolean unwatchAuction(@Argument String auctionId, @Argument String userId) {
        UnwatchCommand cmd = new UnwatchCommand(new AuctionId(auctionId), userId);
        commandBus.send(cmd);
        return true;
    }

    // Schema mappings for nested fields with DataLoader
    @SchemaMapping(typeName = "Auction", field = "item")
    public CompletableFuture<Item> getItem(AuctionDetailsDTO auction, org.dataloader.DataLoader<String, Item> itemDataLoader) {
        return itemDataLoader.load(auction.getItemId());
    }

    @SchemaMapping(typeName = "Auction", field = "seller")
    public CompletableFuture<User> getSeller(AuctionDetailsDTO auction, org.dataloader.DataLoader<String, User> userDataLoader) {
        return userDataLoader.load(String.valueOf(auction.getSellerId()));
    }

    @SchemaMapping(typeName = "Bid", field = "bidder")
    public CompletableFuture<User> getBidder(com.auctionflow.api.dtos.BidHistoryDTO.BidDTO bid, org.dataloader.DataLoader<String, User> userDataLoader) {
        return userDataLoader.load(bid.getBidderId());
    }

    // Subscriptions - placeholder, need to integrate with event system
    // @SubscriptionMapping
    // public Flux<com.auctionflow.api.dtos.BidHistoryDTO.BidDTO> bidPlaced(@Argument String auctionId) {
    //     // Integrate with Kafka or Redis pub/sub
    //     return Flux.empty();
    // }

    // @SubscriptionMapping
    // public Flux<AuctionDetailsDTO> auctionClosed(@Argument String auctionId) {
    //     return Flux.empty();
    // }

    // Input classes
    public static class CreateAuctionInput {
        private String itemId;
        private String categoryId;
        private String auctionType;
        private Float reservePrice;
        private Float buyNowPrice;
        private java.time.LocalDateTime startTime;
        private java.time.LocalDateTime endTime;
        private boolean hiddenReserve;

        // getters and setters
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
        public String getAuctionType() { return auctionType; }
        public void setAuctionType(String auctionType) { this.auctionType = auctionType; }
        public Float getReservePrice() { return reservePrice; }
        public void setReservePrice(Float reservePrice) { this.reservePrice = reservePrice; }
        public Float getBuyNowPrice() { return buyNowPrice; }
        public void setBuyNowPrice(Float buyNowPrice) { this.buyNowPrice = buyNowPrice; }
        public java.time.LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(java.time.LocalDateTime startTime) { this.startTime = startTime; }
        public java.time.LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(java.time.LocalDateTime endTime) { this.endTime = endTime; }
        public boolean isHiddenReserve() { return hiddenReserve; }
        public void setHiddenReserve(boolean hiddenReserve) { this.hiddenReserve = hiddenReserve; }
    }

    public static class PlaceBidInput {
        private Float amount;
        private String idempotencyKey;

        public Float getAmount() { return amount; }
        public void setAmount(Float amount) { this.amount = amount; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    }
}