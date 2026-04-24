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
    private final String ddlAuto;
    private final boolean authCookieSecure;
    private final boolean aiAllowAnonymous;
    private final boolean prometheusPublicAccess;
    private final String prometheusAllowedIpRanges;
    private final String webhookAllowedIpRanges;
    private final String serverShutdown;
    private final boolean healthProbesEnabled;
    private final boolean flywayEnabled;
    private final boolean flywayCleanDisabled;
    private final boolean flywayValidateOnMigrate;
    private final boolean flywayBaselineOnMigrate;
    private final boolean flywayOutOfOrder;

    public ProductionReadinessGuard(Environment environment,
                                    @Value("${app.jwt.secret}") String jwtSecret,
                                    @Value("${app.seed.demo-data.enabled:false}") boolean demoDataEnabled,
                                    @Value("${app.payment.provider:fake}") String paymentProvider,
                                    @Value("${app.payment.toss.secret-key:}") String tossSecretKey,
                                    @Value("${app.frontend.allowed-origins:}") String allowedOrigins,
                                    @Value("${app.frontend.base-url:}") String frontendBaseUrl,
                                    @Value("${spring.datasource.url:}") String datasourceUrl,
                                    @Value("${spring.jpa.hibernate.ddl-auto:}") String ddlAuto,
                                    @Value("${app.auth.cookie.secure:false}") boolean authCookieSecure,
                                    @Value("${app.ai.allow-anonymous:true}") boolean aiAllowAnonymous,
                                    @Value("${app.management.prometheus.public-access:true}") boolean prometheusPublicAccess,
                                    @Value("${app.management.prometheus.allowed-ip-ranges:}") String prometheusAllowedIpRanges,
                                    @Value("${app.payment.toss.webhook.allowed-ip-ranges:}") String webhookAllowedIpRanges,
                                    @Value("${server.shutdown:immediate}") String serverShutdown,
                                    @Value("${management.endpoint.health.probes.enabled:false}") boolean healthProbesEnabled,
                                    @Value("${spring.flyway.enabled:true}") boolean flywayEnabled,
                                    @Value("${spring.flyway.clean-disabled:true}") boolean flywayCleanDisabled,
                                    @Value("${spring.flyway.validate-on-migrate:true}") boolean flywayValidateOnMigrate,
                                    @Value("${spring.flyway.baseline-on-migrate:false}") boolean flywayBaselineOnMigrate,
                                    @Value("${spring.flyway.out-of-order:false}") boolean flywayOutOfOrder) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.demoDataEnabled = demoDataEnabled;
        this.paymentProvider = paymentProvider;
        this.tossSecretKey = tossSecretKey;
        this.allowedOrigins = allowedOrigins;
        this.frontendBaseUrl = frontendBaseUrl;
        this.datasourceUrl = datasourceUrl;
        this.ddlAuto = ddlAuto;
        this.authCookieSecure = authCookieSecure;
        this.aiAllowAnonymous = aiAllowAnonymous;
        this.prometheusPublicAccess = prometheusPublicAccess;
        this.prometheusAllowedIpRanges = prometheusAllowedIpRanges;
        this.webhookAllowedIpRanges = webhookAllowedIpRanges;
        this.serverShutdown = serverShutdown;
        this.healthProbesEnabled = healthProbesEnabled;
        this.flywayEnabled = flywayEnabled;
        this.flywayCleanDisabled = flywayCleanDisabled;
        this.flywayValidateOnMigrate = flywayValidateOnMigrate;
        this.flywayBaselineOnMigrate = flywayBaselineOnMigrate;
        this.flywayOutOfOrder = flywayOutOfOrder;
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

        if (!"validate".equalsIgnoreCase(ddlAuto)) {
            errors.add("spring.jpa.hibernate.ddl-auto must be validate in production.");
        }

        if (!flywayEnabled) {
            errors.add("spring.flyway.enabled must be true in production.");
        }

        if (!flywayCleanDisabled) {
            errors.add("spring.flyway.clean-disabled must be true in production.");
        }

        if (!flywayValidateOnMigrate) {
            errors.add("spring.flyway.validate-on-migrate must be true in production.");
        }

        if (flywayBaselineOnMigrate) {
            errors.add("spring.flyway.baseline-on-migrate must be false in production.");
        }

        if (flywayOutOfOrder) {
            errors.add("spring.flyway.out-of-order must be false in production.");
        }

        if (!authCookieSecure) {
            errors.add("APP_AUTH_COOKIE_SECURE must be true in production.");
        }

        if (aiAllowAnonymous) {
            errors.add("app.ai.allow-anonymous must be false in production.");
        }

        if (prometheusPublicAccess) {
            errors.add("app.management.prometheus.public-access must be false in production.");
        }

        if (prometheusAllowedIpRanges == null || prometheusAllowedIpRanges.isBlank()) {
            errors.add("app.management.prometheus.allowed-ip-ranges must be configured in production.");
        }

        if ("toss".equalsIgnoreCase(paymentProvider) && (webhookAllowedIpRanges == null || webhookAllowedIpRanges.isBlank())) {
            errors.add("app.payment.toss.webhook.allowed-ip-ranges must be configured in production.");
        }

        if (!"graceful".equalsIgnoreCase(serverShutdown)) {
            errors.add("server.shutdown must be graceful in production.");
        }

        if (!healthProbesEnabled) {
            errors.add("management.endpoint.health.probes.enabled must be true in production.");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Production readiness check failed: " + String.join(" ", errors));
        }
    }
}
