package com.test.system.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Filter for logging HTTP requests and responses.
 * Adds request ID to MDC for correlation across logs.
 */
@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final Set<String> SENSITIVE_QUERY_KEYS = new HashSet<>(Arrays.asList(
            "token", "password", "secret", "code", "authorization", "jwt", "api_key", "apikey"
    ));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Generate unique request ID
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(REQUEST_ID, requestId);

        // Wrap request and response for content caching
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();

        try {
            // Log incoming request
            logRequest(wrappedRequest, requestId);

            // Process request
            chain.doFilter(wrappedRequest, wrappedResponse);

            // Log response
            long duration = System.currentTimeMillis() - startTime;
            logResponse(wrappedRequest, wrappedResponse, duration, requestId);

        } finally {
            // Copy cached response content to actual response
            wrappedResponse.copyBodyToResponse();
            
            // Clear MDC
            MDC.clear();
        }
    }

    private void logRequest(HttpServletRequest request, String requestId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String remoteAddr = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("→ HTTP Request [%s] %s %s", requestId, method, uri));
        
        if (queryString != null) {
            logMessage.append("?").append(maskSensitiveQueryString(queryString));
        }
        
        logMessage.append(String.format(" | IP: %s", remoteAddr));
        
        if (userAgent != null && !userAgent.isEmpty()) {
            logMessage.append(String.format(" | UA: %s", userAgent.substring(0, Math.min(50, userAgent.length()))));
        }

        log.info(logMessage.toString());
    }

    private String maskSensitiveQueryString(String queryString) {
        String[] pairs = queryString.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            int eqIdx = pair.indexOf('=');
            String key = eqIdx >= 0 ? pair.substring(0, eqIdx) : pair;

            if (isSensitiveKey(key)) {
                pairs[i] = key + "=***";
            }
        }
        return String.join("&", pairs);
    }

    private boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.trim().toLowerCase();
        return SENSITIVE_QUERY_KEYS.contains(normalized)
                || normalized.endsWith("_token")
                || normalized.endsWith("token");
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response, 
                            long duration, String requestId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        
        String logLevel = getLogLevel(status);
        String statusEmoji = getStatusEmoji(status);
        
        String logMessage = String.format("← HTTP Response [%s] %s %s | Status: %d %s | Duration: %dms",
            requestId, method, uri, status, statusEmoji, duration);

        switch (logLevel) {
            case "ERROR":
                log.error(logMessage);
                break;
            case "WARN":
                log.warn(logMessage);
                break;
            default:
                log.info(logMessage);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle multiple IPs (take first one)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private String getLogLevel(int status) {
        if (status >= 500) {
            return "ERROR";
        } else if (status >= 400) {
            return "WARN";
        } else {
            return "INFO";
        }
    }

    private String getStatusEmoji(int status) {
        if (status >= 200 && status < 300) {
            return "✓";
        } else if (status >= 300 && status < 400) {
            return "↻";
        } else if (status >= 400 && status < 500) {
            return "⚠";
        } else if (status >= 500) {
            return "✗";
        }
        return "";
    }
}
