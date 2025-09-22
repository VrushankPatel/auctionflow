package com.auctionflow.analytics;

import com.auctionflow.analytics.entities.AuditTrail;
import com.auctionflow.analytics.repositories.AuditTrailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditService {

    @Autowired
    private AuditTrailRepository auditTrailRepository;

    public void logEvent(Long userId, String action, String ipAddress, String endpoint, String details, String entityType, Long entityId) {
        AuditTrail auditTrail = new AuditTrail();
        auditTrail.setUserId(userId);
        auditTrail.setAction(action);
        auditTrail.setTimestamp(LocalDateTime.now());
        auditTrail.setIpAddress(ipAddress);
        auditTrail.setEndpoint(endpoint);
        auditTrail.setDetails(details);
        auditTrail.setEntityType(entityType);
        auditTrail.setEntityId(entityId);

        auditTrailRepository.save(auditTrail);
    }

    public void logApiRequest(Long userId, String endpoint, String ipAddress, String details) {
        logEvent(userId, "API_REQUEST", ipAddress, endpoint, details, null, null);
    }

    public void logStateChange(Long userId, String entityType, Long entityId, String changeDetails, String ipAddress) {
        logEvent(userId, "STATE_CHANGE", ipAddress, null, changeDetails, entityType, entityId);
    }

    public void logAdminAction(Long adminId, String action, String details, String ipAddress) {
        logEvent(adminId, "ADMIN_ACTION", ipAddress, null, details, null, null);
    }

    public void logDataAccess(Long userId, String action, String entityType, Long entityId, String ipAddress, String endpoint) {
        logEvent(userId, action, ipAddress, endpoint, "Data access", entityType, entityId);
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void cleanupOldAuditLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        auditTrailRepository.deleteOldEntries(cutoffDate);
    }
}