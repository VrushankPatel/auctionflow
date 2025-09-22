package com.auctionflow.analytics;

import com.auctionflow.analytics.dtos.*;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final RealtimeAnalyticsService analyticsService;
    private final MachineLearningService mlService;
    private final AuditService auditService;
    private final RecommendationService recommendationService;
    private final ABTestingService abTestingService;

    public AnalyticsController(RealtimeAnalyticsService analyticsService, MachineLearningService mlService, AuditService auditService, RecommendationService recommendationService, ABTestingService abTestingService) {
        this.analyticsService = analyticsService;
        this.mlService = mlService;
        this.auditService = auditService;
        this.recommendationService = recommendationService;
        this.abTestingService = abTestingService;
    }

    @GetMapping("/top-auctions")
    public ResponseEntity<List<AuctionAnalytics>> getTopAuctionsByBids(@RequestParam(defaultValue = "10") int limit) {
        auditService.logApiRequest(null, "/analytics/top-auctions", getClientIp(), "limit: " + limit);
        Set<ZSetOperations.TypedTuple<String>> tuples = analyticsService.getTopAuctionsByBids(limit);
        List<AuctionAnalytics> result = tuples.stream()
                .map(tuple -> new AuctionAnalytics(tuple.getValue(), tuple.getScore().longValue()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/trending-categories")
    public ResponseEntity<List<CategoryAnalytics>> getTrendingCategories(@RequestParam(defaultValue = "10") int limit) {
        auditService.logApiRequest(null, "/analytics/trending-categories", getClientIp(), "limit: " + limit);
        Set<ZSetOperations.TypedTuple<String>> tuples = analyticsService.getTrendingCategories(limit);
        List<CategoryAnalytics> result = tuples.stream()
                .map(tuple -> new CategoryAnalytics(tuple.getValue(), tuple.getScore().longValue()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/price-movements")
    public ResponseEntity<List<AuctionPriceAnalytics>> getPriceMovements(@RequestParam(defaultValue = "10") int limit) {
        auditService.logApiRequest(null, "/analytics/price-movements", getClientIp(), "limit: " + limit);
        Set<ZSetOperations.TypedTuple<String>> tuples = analyticsService.getTopAuctionsByPrice(limit);
        List<AuctionPriceAnalytics> result = tuples.stream()
                .map(tuple -> new AuctionPriceAnalytics(tuple.getValue(), tuple.getScore()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/predict-price")
    public Mono<ResponseEntity<PricePredictionResponse>> predictPrice(@RequestBody PricePredictionRequest request) {
        auditService.logApiRequest(null, "/analytics/predict-price", getClientIp(), "request: " + request.toString());
        return mlService.predictPrice(request)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().build()));
    }

    @PostMapping("/recommend-end-time")
    public Mono<ResponseEntity<EndTimeRecommendationResponse>> recommendEndTime(@RequestBody EndTimeRecommendationRequest request) {
        auditService.logApiRequest(null, "/analytics/recommend-end-time", getClientIp(), "request: " + request.toString());
        return mlService.recommendEndTime(request)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().build()));
    }

    @PostMapping("/score-fraud")
    public Mono<ResponseEntity<FraudScoreResponse>> scoreFraud(@RequestBody FraudScoreRequest request) {
        auditService.logApiRequest(null, "/analytics/score-fraud", getClientIp(), "request: " + request.toString());
        return mlService.scoreFraud(request)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().build()));
    }

    @PostMapping("/recommend")
    public ResponseEntity<RecommendationResponse> getRecommendations(@RequestBody RecommendationRequest request) {
        auditService.logApiRequest(null, "/analytics/recommend", getClientIp(), "request: " + request.toString());
        String userId = request.getUserId() != null ? request.getUserId().toString() : "anonymous";
        String variant = abTestingService.assignVariant("recommendation_algorithm", userId);
        List<Long> recommended;
        if (request.getAuctionId() != null) {
            // Similar auctions - use collaborative
            recommended = recommendationService.getSimilarAuctionsCollaborative(request.getAuctionId(), request.getLimit());
        } else if (request.getUserId() != null) {
            // Personalized recommendations - use variant
            if ("collaborative".equals(variant)) {
                recommended = recommendationService.getRecommendedAuctions(request.getUserId(), request.getLimit());
            } else if ("content_based".equals(variant)) {
                // For simplicity, use hybrid
                recommended = recommendationService.getRecommendedAuctions(request.getUserId(), request.getLimit());
            } else {
                recommended = recommendationService.getRecommendedAuctions(request.getUserId(), request.getLimit());
            }
        } else {
            return ResponseEntity.badRequest().build();
        }
        // Log impression
        abTestingService.logEvent("recommendation_algorithm", variant, userId, "impression", Map.of("count", recommended.size()));
        RecommendationResponse response = new RecommendationResponse();
        response.setRecommendedAuctionIds(recommended);
        return ResponseEntity.ok(response);
    }

    private String getClientIp() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        return request.getRemoteAddr();
    }

    public static class AuctionAnalytics {
        private String auctionId;
        private long bidCount;

        public AuctionAnalytics(String auctionId, long bidCount) {
            this.auctionId = auctionId;
            this.bidCount = bidCount;
        }

        // getters
        public String getAuctionId() { return auctionId; }
        public long getBidCount() { return bidCount; }
    }

    public static class CategoryAnalytics {
        private String categoryId;
        private long bidCount;

        public CategoryAnalytics(String categoryId, long bidCount) {
            this.categoryId = categoryId;
            this.bidCount = bidCount;
        }

        // getters
        public String getCategoryId() { return categoryId; }
        public long getBidCount() { return bidCount; }
    }

    public static class AuctionPriceAnalytics {
        private String auctionId;
        private double currentPrice;

        public AuctionPriceAnalytics(String auctionId, double currentPrice) {
            this.auctionId = auctionId;
            this.currentPrice = currentPrice;
        }

        // getters
        public String getAuctionId() { return auctionId; }
        public double getCurrentPrice() { return currentPrice; }
    }
}