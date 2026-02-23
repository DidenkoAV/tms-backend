package com.test.system.utils.logging;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for structured logging with MDC (Mapped Diagnostic Context).
 * Provides helper methods for consistent logging across the application.
 */
public class LoggingUtils {

    private LoggingUtils() {
        // Utility class
    }

    /**
     * Add user ID to MDC for correlation
     */
    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put("userId", userId.toString());
        }
    }

    /**
     * Add user email to MDC
     */
    public static void setUserEmail(String email) {
        if (email != null) {
            MDC.put("userEmail", email);
        }
    }

    /**
     * Add group ID to MDC
     */
    public static void setGroupId(Long groupId) {
        if (groupId != null) {
            MDC.put("groupId", groupId.toString());
        }
    }

    /**
     * Add project ID to MDC
     */
    public static void setProjectId(Long projectId) {
        if (projectId != null) {
            MDC.put("projectId", projectId.toString());
        }
    }

    /**
     * Clear all MDC values
     */
    public static void clearMDC() {
        MDC.clear();
    }

    /**
     * Log authentication event
     */
    public static void logAuthEvent(Logger logger, String event, String email, boolean success) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("event", event);
        logData.put("email", email);
        logData.put("success", success);
        
        if (success) {
            logger.info("Auth Event: {} | Email: {} | Success: {}", event, email, success);
        } else {
            logger.warn("Auth Event: {} | Email: {} | Success: {}", event, email, success);
        }
    }

    /**
     * Log security event
     */
    public static void logSecurityEvent(Logger logger, String event, String details) {
        logger.warn("Security Event: {} | Details: {}", event, details);
    }

    /**
     * Log business operation
     */
    public static void logBusinessOperation(Logger logger, String operation, String entity, Long entityId, String action) {
        logger.info("Business Operation: {} | Entity: {} | ID: {} | Action: {}", 
            operation, entity, entityId, action);
    }

    /**
     * Log database operation
     */
    public static void logDatabaseOperation(Logger logger, String operation, String table, Long recordId) {
        logger.debug("DB Operation: {} | Table: {} | Record ID: {}", operation, table, recordId);
    }

    /**
     * Log external API call
     */
    public static void logExternalApiCall(Logger logger, String service, String endpoint, String method, long duration) {
        logger.info("External API: {} | Endpoint: {} | Method: {} | Duration: {}ms", 
            service, endpoint, method, duration);
    }

    /**
     * Log performance metric
     */
    public static void logPerformance(Logger logger, String operation, long duration) {
        if (duration > 1000) {
            logger.warn("Performance: {} | Duration: {}ms (SLOW)", operation, duration);
        } else {
            logger.debug("Performance: {} | Duration: {}ms", operation, duration);
        }
    }

    /**
     * Log validation error
     */
    public static void logValidationError(Logger logger, String field, String error, Object value) {
        logger.warn("Validation Error: Field: {} | Error: {} | Value: {}", field, error, value);
    }

    /**
     * Log data change for audit
     */
    public static void logDataChange(Logger logger, String entity, Long entityId, String field, 
                                     Object oldValue, Object newValue, Long userId) {
        logger.info("Data Change: Entity: {} | ID: {} | Field: {} | Old: {} | New: {} | User: {}", 
            entity, entityId, field, oldValue, newValue, userId);
    }

    /**
     * Log email sent
     */
    public static void logEmailSent(Logger logger, String to, String subject, boolean success) {
        if (success) {
            logger.info("Email Sent: To: {} | Subject: {} | Success: true", to, subject);
        } else {
            logger.error("Email Failed: To: {} | Subject: {} | Success: false", to, subject);
        }
    }

    /**
     * Log file operation
     */
    public static void logFileOperation(Logger logger, String operation, String filename, long size) {
        logger.info("File Operation: {} | File: {} | Size: {} bytes", operation, filename, size);
    }

    /**
     * Log cache operation
     */
    public static void logCacheOperation(Logger logger, String operation, String key, boolean hit) {
        logger.debug("Cache: {} | Key: {} | Hit: {}", operation, key, hit);
    }

    /**
     * Create structured log message
     */
    public static String structuredLog(String message, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder(message);
        if (context != null && !context.isEmpty()) {
            sb.append(" | ");
            context.forEach((key, value) -> 
                sb.append(key).append(": ").append(value).append(" | ")
            );
            // Remove trailing " | "
            sb.setLength(sb.length() - 3);
        }
        return sb.toString();
    }
}

