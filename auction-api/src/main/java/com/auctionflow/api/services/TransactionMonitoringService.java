package com.auctionflow.api.services;

import com.auctionflow.api.entities.ComplianceCheck;
import com.auctionflow.api.repositories.ComplianceCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class TransactionMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionMonitoringService.class);

    @Autowired
    private ComplianceCheckRepository complianceCheckRepository;

    @Autowired
    private SuspiciousActivityService suspiciousActivityService;

    // Thresholds for suspicious activity detection
    private static final BigDecimal HIGH_VALUE_THRESHOLD = BigDecimal.valueOf(10000.0);
    private static final int RAPID_BIDS_THRESHOLD = 10; // bids per minute
    private static final BigDecimal UNUSUAL_BID_INCREASE_THRESHOLD = BigDecimal.valueOf(2.0); // 200% increase

    /**
     * Monitor bid placement for suspicious patterns
     */
    public void monitorBidPlacement(Long userId, Long auctionId, BigDecimal bidAmount, Instant bidTime) {
        logger.debug("Monitoring bid placement: user={}, auction={}, amount={}, time={}",
                    userId, auctionId, bidAmount, bidTime);

        // Check for high-value bids
        if (bidAmount.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            flagSuspiciousActivity(userId, "HIGH_VALUE_BID",
                Map.of("auctionId", auctionId.toString(), "bidAmount", bidAmount.toString()));
        }

        // Check for rapid bidding patterns (would need bid history - placeholder)
        checkRapidBidding(userId, auctionId, bidTime);

        // Check for unusual bid increases (would need previous bids - placeholder)
        checkUnusualBidIncrease(userId, auctionId, bidAmount);

        // Create transaction monitoring compliance check
        createTransactionCheck(userId, "BID_PLACEMENT", bidAmount);
    }

    /**
     * Monitor auction wins for high-value transactions
     */
    public void monitorAuctionWin(Long userId, Long auctionId, BigDecimal finalPrice) {
        logger.info("Monitoring auction win: user={}, auction={}, finalPrice={}",
                   userId, auctionId, finalPrice);

        if (finalPrice.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            flagSuspiciousActivity(userId, "HIGH_VALUE_WIN",
                Map.of("auctionId", auctionId.toString(), "finalPrice", finalPrice.toString()));
        }

        createTransactionCheck(userId, "AUCTION_WIN", finalPrice);
    }

    /**
     * Monitor payment processing
     */
    public void monitorPayment(Long userId, Long auctionId, BigDecimal amount, String paymentMethod) {
        logger.info("Monitoring payment: user={}, auction={}, amount={}, method={}",
                   userId, auctionId, amount, paymentMethod);

        // Flag suspicious payment methods or patterns
        if ("CRYPTOCURRENCY".equals(paymentMethod) && amount.compareTo(BigDecimal.valueOf(5000.0)) >= 0) {
            flagSuspiciousActivity(userId, "CRYPTO_PAYMENT_HIGH_VALUE",
                Map.of("auctionId", auctionId.toString(), "amount", amount.toString(), "method", paymentMethod));
        }

        createTransactionCheck(userId, "PAYMENT", amount);
    }

    private void checkRapidBidding(Long userId, Long auctionId, Instant bidTime) {
        // Placeholder - in real implementation, query recent bids by user
        // If user has placed more than RAPID_BIDS_THRESHOLD bids in the last minute, flag as suspicious

        // For now, just log
        logger.debug("Checking rapid bidding for user {} on auction {}", userId, auctionId);
    }

    private void checkUnusualBidIncrease(Long userId, Long auctionId, BigDecimal bidAmount) {
        // Placeholder - in real implementation, compare with previous bids in auction
        // If bid is UNUSUAL_BID_INCREASE_THRESHOLD times higher than previous high bid, flag

        logger.debug("Checking unusual bid increase for user {} on auction {} with amount {}",
                    userId, auctionId, bidAmount);
    }

    private void flagSuspiciousActivity(Long userId, String activityType, Map<String, String> details) {
        logger.warn("Flagging suspicious activity: user={}, type={}, details={}",
                   userId, activityType, details);

        // Use existing SuspiciousActivityService
        suspiciousActivityService.recordSuspiciousActivity(userId, activityType, details.toString());

        // Create compliance check
        ComplianceCheck check = new ComplianceCheck();
        check.setUserId(userId);
        check.setCheckType(ComplianceCheck.CheckType.TRANSACTION_MONITORING);
        check.setStatus(ComplianceCheck.CheckStatus.REQUIRES_REVIEW);
        check.setRiskScore(BigDecimal.valueOf(85.0)); // High risk
        check.setDetails("{\"activityType\": \"" + activityType + "\", \"details\": " + details.toString() + "}");

        complianceCheckRepository.save(check);
    }

    private void createTransactionCheck(Long userId, String transactionType, BigDecimal amount) {
        ComplianceCheck check = new ComplianceCheck();
        check.setUserId(userId);
        check.setCheckType(ComplianceCheck.CheckType.TRANSACTION_MONITORING);
        check.setStatus(ComplianceCheck.CheckStatus.PASSED); // Normal transaction
        check.setRiskScore(BigDecimal.valueOf(10.0)); // Low risk for normal transactions
        check.setDetails("{\"transactionType\": \"" + transactionType + "\", \"amount\": " + amount + "}");

        complianceCheckRepository.save(check);
    }

    /**
     * Generate transaction monitoring report for a user
     */
    public TransactionMonitoringReport generateReport(Long userId) {
        List<ComplianceCheck> checks = complianceCheckRepository.findByUserIdAndCheckType(
            userId, ComplianceCheck.CheckType.TRANSACTION_MONITORING);

        long suspiciousActivities = checks.stream()
            .filter(check -> check.getStatus() == ComplianceCheck.CheckStatus.REQUIRES_REVIEW)
            .count();

        long totalTransactions = checks.size();

        BigDecimal averageRiskScore = checks.stream()
            .filter(check -> check.getRiskScore() != null)
            .map(ComplianceCheck::getRiskScore)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(Math.max(totalTransactions, 1)), 2, BigDecimal.ROUND_HALF_UP);

        return new TransactionMonitoringReport(userId, totalTransactions, suspiciousActivities, averageRiskScore);
    }

    /**
     * Check if user has exceeded monitoring thresholds
     */
    public boolean exceedsMonitoringThresholds(Long userId) {
        TransactionMonitoringReport report = generateReport(userId);

        // Flag if more than 3 suspicious activities in last 30 days
        return report.getSuspiciousActivities() >= 3;
    }

    public static class TransactionMonitoringReport {
        private final Long userId;
        private final long totalTransactions;
        private final long suspiciousActivities;
        private final BigDecimal averageRiskScore;

        public TransactionMonitoringReport(Long userId, long totalTransactions,
                                        long suspiciousActivities, BigDecimal averageRiskScore) {
            this.userId = userId;
            this.totalTransactions = totalTransactions;
            this.suspiciousActivities = suspiciousActivities;
            this.averageRiskScore = averageRiskScore;
        }

        public Long getUserId() { return userId; }
        public long getTotalTransactions() { return totalTransactions; }
        public long getSuspiciousActivities() { return suspiciousActivities; }
        public BigDecimal getAverageRiskScore() { return averageRiskScore; }
    }
}