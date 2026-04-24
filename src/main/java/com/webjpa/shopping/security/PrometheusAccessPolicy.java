package com.webjpa.shopping.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PrometheusAccessPolicy {

    private final boolean publicAccess;
    private final ClientAddressResolver clientAddressResolver;
    private final IpRangeMatcher allowedIpRanges;

    public PrometheusAccessPolicy(
            ClientAddressResolver clientAddressResolver,
            @Value("${app.management.prometheus.public-access:true}") boolean publicAccess,
            @Value("${app.management.prometheus.allowed-ip-ranges:}") String allowedIpRanges
    ) {
        this.publicAccess = publicAccess;
        this.clientAddressResolver = clientAddressResolver;
        this.allowedIpRanges = new IpRangeMatcher(allowedIpRanges);
    }

    public boolean isAllowed(HttpServletRequest request) {
        if (publicAccess) {
            return true;
        }
        return allowedIpRanges.contains(clientAddressResolver.resolveClientIp(request));
    }
}
