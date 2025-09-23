// package com.auctionflow.api;

// import com.auctionflow.api.services.AutomatedBiddingService;
// import com.auctionflow.bidding.strategies.AutomatedBidStrategy;
// import com.auctionflow.bidding.strategies.StrategyType;
// import com.auctionflow.core.domain.valueobjects.AuctionId;
// import com.auctionflow.core.domain.valueobjects.BidderId;
// import com.auctionflow.core.domain.valueobjects.Money;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;
// import java.util.Map;

// /**
//  * REST controller for automated bidding strategies
//  */
// // @RestController
// // @RequestMapping("/api/v1/automated-bidding")
// // public class AutomatedBiddingController {

//     private final AutomatedBiddingService automatedBiddingService;

//     public AutomatedBiddingController(AutomatedBiddingService automatedBiddingService) {
//         this.automatedBiddingService = automatedBiddingService;
//     }

//     @PostMapping("/strategies")
//     public ResponseEntity<AutomatedBidStrategy> createStrategy(
//             @RequestParam Long auctionId,
//             @RequestParam String bidderId,
//             @RequestParam StrategyType strategyType,
//             @RequestParam Double maxBid,
//             @RequestBody Map<String, Object> parameters) {

//         AuctionId auction = new AuctionId(auctionId);
//         BidderId bidder = BidderId.fromString(bidderId);
//         Money maxBidAmount = new Money(maxBid);

//         AutomatedBidStrategy strategy = automatedBiddingService.createStrategy(
//             auction, bidder, strategyType, parameters, maxBidAmount);

//         return ResponseEntity.ok(strategy);
//     }

//     @GetMapping("/strategies")
//     public ResponseEntity<List<AutomatedBidStrategy>> getStrategies(
//             @RequestParam String bidderId,
//             @RequestParam(defaultValue = "true") boolean activeOnly) {

//         List<AutomatedBidStrategy> strategies = automatedBiddingService.getStrategiesForBidder(bidderId, activeOnly);
//         return ResponseEntity.ok(strategies);
//     }

//     @DeleteMapping("/strategies/{strategyId}")
//     public ResponseEntity<Void> deactivateStrategy(@PathVariable Long strategyId) {
//         automatedBiddingService.deactivateStrategy(strategyId);
//         return ResponseEntity.noContent().build();
//     }
// // }