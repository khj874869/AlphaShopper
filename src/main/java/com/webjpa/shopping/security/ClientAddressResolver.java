package com.webjpa.shopping.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientAddressResolver {

    private final boolean trustForwardedHeaders;

    public ClientAddressResolver(@Value("${app.network.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        if (trustForwardedHeaders) {
            String forwardedFor = firstHeaderValue(request.getHeader("X-Forwarded-For"));
            if (!forwardedFor.isBlank()) {
                return forwardedFor;
            }

            String realIp = firstHeaderValue(request.getHeader("X-Real-IP"));
            if (!realIp.isBlank()) {
                return realIp;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr.trim();
    }

    private String firstHeaderValue(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return "";
        }
        return headerValue.split(",", 2)[0].trim();
    }
}
