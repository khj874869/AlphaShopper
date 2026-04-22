package com.webjpa.shopping.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);
    private static final int MAX_REQUEST_ID_LENGTH = 100;
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]+");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAt = System.nanoTime();
        LoggingContext.putRequestId(requestId);
        response.setHeader(LoggingContext.REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("event=http.request.completed requestId={} method={} path={} status={} durationMs={}",
                    requestId,
                    request.getMethod(),
                    LogValues.safe(request.getRequestURI()),
                    response.getStatus(),
                    durationMs);
            LoggingContext.clearRequestId();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(LoggingContext.REQUEST_ID_HEADER);
        String normalizedRequestId = normalizeRequestId(requestId);
        if (!normalizedRequestId.isBlank()) {
            return normalizedRequestId;
        }
        return UUID.randomUUID().toString();
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return "";
        }

        String sanitized = requestId.trim();
        if (sanitized.length() > MAX_REQUEST_ID_LENGTH || !REQUEST_ID_PATTERN.matcher(sanitized).matches()) {
            return "";
        }
        return sanitized;
    }
}
