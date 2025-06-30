package io.github.bluething.java.bolttrack.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Component
@Order(1)
class RequestResponseLoggingFilter extends OncePerRequestFilter {
    private static final int MAX_PAYLOAD_LENGTH = 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        logRequest(requestWrapper);
        filterChain.doFilter(requestWrapper, responseWrapper);
        logResponse(responseWrapper);
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String contentType = request.getContentType();

        // Log basic request info at INFO level (Sleuth will automatically add trace info)
        log.info("HTTP Request - method: {}, uri: {}, clientIp: {}",
                request.getMethod(), request.getRequestURI(), clientIp);

        // Log detailed request info at DEBUG level
        if (log.isDebugEnabled()) {
            log.debug("Request details - headers: {}, contentType: {}, userAgent: {}",
                    getHeadersAsString(request), contentType, sanitizeUserAgent(userAgent));

            // Log request body for POST/PUT requests (only non-sensitive endpoints)
            if (shouldLogRequestBody(request)) {
                String requestBody = getRequestBody(request);
                log.debug("Request body - body: {}", requestBody);
            }
        }
    }

    private void logResponse(ContentCachingResponseWrapper response) {
        int statusCode = response.getStatus();
        String contentType = response.getContentType();

        // Determine log level based on status code (Sleuth will add trace info)
        if (statusCode >= 500) {
            log.error("HTTP Response - status: {}", statusCode);
        } else if (statusCode >= 400) {
            log.warn("HTTP Response - status: {}", statusCode);
        } else {
            log.info("HTTP Response - status: {}", statusCode);
        }

        // Log detailed response info at DEBUG level
        if (log.isDebugEnabled()) {
            log.debug("Response details - status: {}, contentType: {}, size: {} bytes",
                    statusCode, contentType, response.getContentSize());

            // Log response body for error cases or if explicitly enabled
            if (shouldLogResponseBody(response)) {
                String responseBody = getResponseBody(response);
                log.debug("Response body - body: {}", responseBody);
            }
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null) {
            return "unknown";
        }
        return userAgent.length() > 200 ? userAgent.substring(0, 200) + "..." : userAgent;
    }

    private String getHeadersAsString(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            // Skip sensitive headers
            if (!isSensitiveHeader(headerName)) {
                headers.append(headerName).append(": ").append(request.getHeader(headerName)).append("; ");
            }
        });
        return headers.toString();
    }

    private boolean isSensitiveHeader(String headerName) {
        String lowerHeaderName = headerName.toLowerCase();
        return lowerHeaderName.contains("authorization") ||
                lowerHeaderName.contains("cookie") ||
                lowerHeaderName.contains("password") ||
                lowerHeaderName.contains("token");
    }

    private boolean shouldLogRequestBody(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // Only log body for POST/PUT requests and non-sensitive endpoints
        return ("POST".equals(method) || "PUT".equals(method)) &&
                !uri.contains("/auth") &&
                !uri.contains("/login") &&
                !uri.contains("/password");
    }

    private boolean shouldLogResponseBody(ContentCachingResponseWrapper response) {
        int status = response.getStatus();
        // Log response body for error cases (4xx, 5xx)
        return status >= 400;
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            return body.length() > MAX_PAYLOAD_LENGTH ?
                    body.substring(0, MAX_PAYLOAD_LENGTH) + "... [truncated]" : body;
        }
        return "[empty]";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            return body.length() > MAX_PAYLOAD_LENGTH ?
                    body.substring(0, MAX_PAYLOAD_LENGTH) + "... [truncated]" : body;
        }
        return "[empty]";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip logging for actuator endpoints and static resources
        return path.startsWith("/actuator") ||
                path.startsWith("/static") ||
                path.startsWith("/css") ||
                path.startsWith("/js") ||
                path.startsWith("/images");
    }
}
