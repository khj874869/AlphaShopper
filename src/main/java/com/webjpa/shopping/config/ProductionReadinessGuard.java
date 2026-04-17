package com.webjpa.shopping.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ProductionReadinessGuard {

    static final String DEFAULT_LOCAL_JWT_SECRET = "alphashopper-local-dev-jwt-secret-key-change-me";

    private final Environment environment;
    private final String jwtSecret;
    private final boolean demoDataEnabled;
    private final String paymentProvider;
    private final String tossSecretKey;
    private final String allowedOrigins;
    private final String frontendBaseUrl;
    private final String datasourceUrl;
    private final boolean authCookieSecure;

    public ProductionReadinessGuard(Environment environment,
                                    @Value("${app.jwt.secret}") String jwtSecret,
                                    @Value("${app.seed.demo-data.enabled:false}") boolean demoDataEnabled,
                                    @Value("${app.payment.provider:fake}") String paymentProvider,
                                    @Value("${app.payment.toss.secret-key:}") String tossSecretKey,
                                    @Value("${app.frontend.allowed-origins:}") String allowedOrigins,
                                    @Value("${app.frontend.base-url:}") String frontendBaseUrl,
                                    @Value("${spring.datasource.url:}") String datasourceUrl,
                                    @Value("${app.auth.cookie.secure:false}") boolean authCookieSecure) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.demoDataEnabled = demoDataEnabled;
        this.paymentProvider = paymentProvider;
        this.tossSecretKey = tossSecretKey;
        this.allowedOrigins = allowedOrigins;
        this.frontendBaseUrl = frontendBaseUrl;
        this.datasourceUrl = datasourceUrl;
        this.authCookieSecure = authCookieSecure;
    }

    @PostConstruct
    void validate() {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }

        List<String> errors = new ArrayList<>();

        if (DEFAULT_LOCAL_JWT_SECRET.equals(jwtSecret)) {
            errors.add("APP_JWT_SECRET must be set to a secure non-default value.");
        }

        if (demoDataEnabled) {
            errors.add("app.seed.demo-data.enabled must be false.");
        }

        if ("fake".equalsIgnoreCase(paymentProvider)) {
            errors.add("app.payment.provider=fake is not allowed.");
        }

        if ("toss".equalsIgnoreCase(paymentProvider) && (tossSecretKey == null || tossSecretKey.isBlank())) {
            errors.add("app.payment.toss.secret-key must be set when using Toss Payments.");
        }

        if (allowedOrigins.contains("localhost") || allowedOrigins.contains("127.0.0.1")) {
            errors.add("app.frontend.allowed-origins must not include localhost entries.");
        }

        if (frontendBaseUrl.isBlank() || frontendBaseUrl.contains("localhost") || frontendBaseUrl.contains("127.0.0.1")) {
            errors.add("app.frontend.base-url must be set to the public storefront URL.");
        }

        if (datasourceUrl.contains("jdbc:h2:mem")) {
            errors.add("Production profile cannot use the in-memory H2 datasource.");
        }

        if (!authCookieSecure) {
            errors.add("APP_AUTH_COOKIE_SECURE must be true in production.");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Production readiness check failed: " + String.join(" ", errors));
        }
    }
}
