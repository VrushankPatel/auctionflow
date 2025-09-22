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
import com.auctionflow.api.services.UserService;
import com.auctionflow.core.domain.commands.*;
import com.auctionflow.core.domain.valueobjects.AntiSnipePolicy;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.Money;
import com.auctionflow.events.command.CommandBus;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Controller
public class AuctionGraphQLController {

    private final CommandBus commandBus;
    private final ListActiveAuctionsQueryHandler listHandler;
    private final GetAuctionDetailsQueryHandler detailsHandler;
    private final GetBidHistoryQueryHandler bidHistoryHandler;
    private final UserService userService;
    private final ItemValidationService itemValidationService;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public AuctionGraphQLController(CommandBus commandBus,
                                    ListActiveAuctionsQueryHandler listHandler,
                                    GetAuctionDetailsQueryHandler detailsHandler,
                                    GetBidHistoryQueryHandler bidHistoryHandler,
                                    UserService userService,
                                    ItemValidationService itemValidationService,
                                    ItemRepository itemRepository,
                                    UserRepository userRepository) {
        this.commandBus = commandBus;
        this.listHandler = listHandler;
        this.detailsHandler = detailsHandler;
        this.bidHistoryHandler = bidHistoryHandler;
        this.userService = userService;
        this.itemValidationService = itemValidationService;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @QueryMapping
    public ActiveAuctionsDTO auctions(@Argument String category, @Argument String sellerId, @Argument Integer page, @Argument Integer size) {
        ListActiveAuctionsQuery query = new ListActiveAuctionsQuery(Optional.ofNullable(category), Optional.ofNullable(sellerId), page != null ? page : 0, size != null ? size : 10);
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
        return userRepository.findById(UUID.fromString(id)).orElse(null);
    }

    @MutationMapping
    public AuctionDetailsDTO createAuction(@Argument CreateAuctionInput input) {
        // Similar to REST controller
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByEmail(email);
        if (user == null || !userService.isKycVerified(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        Item item = itemRepository.findById(UUID.fromString(input.getItemId())).orElse(null);
        if (item == null) {
            throw new RuntimeException("Item not found");
        }
        ItemValidationService.ValidationResult validation = itemValidationService.validateItem(item.getCategoryId(), item.getTitle(), item.getDescription(), item.getBrand(), item.getSerialNumber());
        if (!validation.isValid()) {
            throw new RuntimeException("Invalid item");
        }

        Money reservePrice = input.getReservePrice() != null ? new Money(BigDecimal.valueOf(input.getReservePrice())) : null;
        Money buyNowPrice = input.getBuyNowPrice() != null ? new Money(BigDecimal.valueOf(input.getBuyNowPrice())) : null;
        CreateAuctionCommand cmd = new CreateAuctionCommand(
            new com.auctionflow.core.domain.valueobjects.ItemId(UUID.fromString(input.getItemId())),
            input.getCategoryId(),
            input.getAuctionType(),
            reservePrice,
            buyNowPrice,
            input.getStartTime(),
            input.getEndTime(),
            AntiSnipePolicy.NONE,
            input.isHiddenReserve()
        );
        commandBus.send(cmd);

        // Return the created auction - assuming command returns ID or we query it
        // For simplicity, query by itemId or something, but actually need to get the auction ID
        // This is a simplification; in real implementation, command should return the auction ID
        return null; // TODO: implement properly
    }

    @MutationMapping
    public AuctionDetailsDTO placeBid(@Argument String auctionId, @Argument PlaceBidInput input) {
        UUID bidderId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Money amount = new Money(BigDecimal.valueOf(input.getAmount()));
        PlaceBidCommand cmd = new PlaceBidCommand(new AuctionId(auctionId), bidderId, amount, input.getIdempotencyKey());
        commandBus.send(cmd);
        // Return updated auction
        GetAuctionDetailsQuery query = new GetAuctionDetailsQuery(auctionId);
        return detailsHandler.handle(query).orElse(null);
    }

    @MutationMapping
    public Boolean watchAuction(@Argument String auctionId, @Argument String userId) {
        WatchCommand cmd = new WatchCommand(new AuctionId(auctionId), UUID.fromString(userId));
        commandBus.send(cmd);
        return true;
    }

    @MutationMapping
    public Boolean unwatchAuction(@Argument String auctionId, @Argument String userId) {
        UnwatchCommand cmd = new UnwatchCommand(new AuctionId(auctionId), UUID.fromString(userId));
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
        return userDataLoader.load(auction.getSellerId());
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