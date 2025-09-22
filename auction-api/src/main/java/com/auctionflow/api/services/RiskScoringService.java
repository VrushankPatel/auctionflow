package com.auctionflow.api.services;

import com.auctionflow.api.entities.ComplianceCheck;
import com.auctionflow.api.entities.User;
import com.auctionflow.api.repositories.ComplianceCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RiskScoringService {

    private static final Logger logger = LoggerFactory.getLogger(RiskScoringService.class);

    @Autowired
    private ComplianceCheckRepository complianceCheckRepository;

    @Autowired
    private UserService userService;

    /**
     * Calculate comprehensive risk score for a user
     * Score ranges from 0 (low risk) to 100 (high risk)
     */
    public BigDecimal calculateRiskScore(Long userId) {
        logger.info("Calculating risk score for user {}", userId);

        BigDecimal baseScore = BigDecimal.valueOf(50.0); // Neutral starting point

        // Factor 1: KYC/Identity verification status
        baseScore = baseScore.add(calculateKycRiskFactor(userId));

        // Factor 2: Account age and activity patterns
        baseScore = baseScore.add(calculateAccountRiskFactor(userId));

        // Factor 3: Transaction patterns (placeholder - would need transaction data)
        baseScore = baseScore.add(calculateTransactionRiskFactor(userId));

        // Factor 4: Compliance history
        baseScore = baseScore.add(calculateComplianceHistoryRiskFactor(userId));

        // Ensure score is between 0 and 100
        BigDecimal finalScore = baseScore.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100.0));

        logger.info("Risk score for user {}: {}", userId, finalScore);

        // Store the risk assessment
        storeRiskAssessment(userId, finalScore);

        return finalScore;
    }

    private BigDecimal calculateKycRiskFactor(Long userId) {
        try {
            User user = userService.getUserById(userId); // Assuming this method exists
            if (user == null) return BigDecimal.valueOf(50.0); // High risk if user not found

            switch (user.getKycStatus()) {
                case VERIFIED:
                    return BigDecimal.valueOf(-30.0); // Significant risk reduction
                case PENDING:
                    return BigDecimal.valueOf(10.0); // Moderate risk increase
                case REJECTED:
                    return BigDecimal.valueOf(40.0); // High risk increase
                default:
                    return BigDecimal.valueOf(20.0);
            }
        } catch (Exception e) {
            logger.warn("Error calculating KYC risk factor for user {}: {}", userId, e.getMessage());
            return BigDecimal.valueOf(25.0); // Moderate risk increase on error
        }
    }

    private BigDecimal calculateAccountRiskFactor(Long userId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) return BigDecimal.valueOf(50.0);

            long accountAgeDays = ChronoUnit.DAYS.between(user.getCreatedAt(), Instant.now());

            if (accountAgeDays < 1) {
                return BigDecimal.valueOf(30.0); // Very new account - high risk
            } else if (accountAgeDays < 7) {
                return BigDecimal.valueOf(20.0); // New account
            } else if (accountAgeDays < 30) {
                return BigDecimal.valueOf(10.0); // Relatively new
            } else if (accountAgeDays < 90) {
                return BigDecimal.valueOf(0.0); // Established
            } else {
                return BigDecimal.valueOf(-10.0); // Well-established - lower risk
            }
        } catch (Exception e) {
            logger.warn("Error calculating account risk factor for user {}: {}", userId, e.getMessage());
            return BigDecimal.valueOf(15.0);
        }
    }

    private BigDecimal calculateTransactionRiskFactor(Long userId) {
        // Placeholder - in a real implementation, this would analyze:
        // - Number of transactions
        // - Transaction amounts and patterns
        // - Failed payment attempts
        // - Geographic patterns
        // - Time patterns (e.g., unusual hours)

        // For now, return a neutral score
        return BigDecimal.valueOf(0.0);
    }

    private BigDecimal calculateComplianceHistoryRiskFactor(Long userId) {
        try {
            List<ComplianceCheck> checks = complianceCheckRepository.findByUserIdOrderByCreatedAtDesc(userId);

            if (checks.isEmpty()) {
                return BigDecimal.valueOf(15.0); // No compliance history - moderate risk
            }

            long failedChecks = checks.stream()
                .filter(check -> check.getStatus() == ComplianceCheck.CheckStatus.FAILED)
                .count();

            long totalChecks = checks.size();
            BigDecimal failureRate = BigDecimal.valueOf(failedChecks).divide(BigDecimal.valueOf(totalChecks), 2, RoundingMode.HALF_UP);

            // Increase risk based on failure rate
            return failureRate.multiply(BigDecimal.valueOf(25.0));

        } catch (Exception e) {
            logger.warn("Error calculating compliance history risk factor for user {}: {}", userId, e.getMessage());
            return BigDecimal.valueOf(10.0);
        }
    }

    private void storeRiskAssessment(Long userId, BigDecimal riskScore) {
        try {
            ComplianceCheck assessment = new ComplianceCheck();
            assessment.setUserId(userId);
            assessment.setCheckType(ComplianceCheck.CheckType.RISK_ASSESSMENT);
            assessment.setStatus(ComplianceCheck.CheckStatus.PASSED); // Assessment completed
            assessment.setRiskScore(riskScore);
            assessment.setDetails("{\"assessmentType\": \"automated\", \"factors\": [\"kyc\", \"account_age\", \"compliance_history\"]}");

            complianceCheckRepository.save(assessment);
        } catch (Exception e) {
            logger.error("Failed to store risk assessment for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Check if user exceeds risk threshold and requires manual review
     */
    public boolean requiresManualReview(Long userId) {
        BigDecimal riskScore = calculateRiskScore(userId);
        return riskScore.compareTo(BigDecimal.valueOf(75.0)) >= 0;
    }

    /**
     * Get risk level description
     */
    public String getRiskLevel(BigDecimal riskScore) {
        if (riskScore.compareTo(BigDecimal.valueOf(80.0)) >= 0) {
            return "HIGH";
        } else if (riskScore.compareTo(BigDecimal.valueOf(60.0)) >= 0) {
            return "MEDIUM";
        } else if (riskScore.compareTo(BigDecimal.valueOf(40.0)) >= 0) {
            return "LOW";
        } else {
            return "VERY_LOW";
        }
    }
}