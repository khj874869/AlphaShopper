package com.webjpa.shopping.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionReadinessGuardTest {

    @Test
    void validate_whenProdSettingsAreSafe_passes() {
        ProductionReadinessGuard guard = guard(
                true,
                true,
                true,
                false,
                false,
                "validate"
        );

        assertThatCode(guard::validate).doesNotThrowAnyException();
    }

    @Test
    void validate_whenProdAllowsFlywayClean_failsStartup() {
        ProductionReadinessGuard guard = guard(
                true,
                false,
                true,
                false,
                false,
                "validate"
        );

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.flyway.clean-disabled must be true in production.");
    }

    @Test
    void validate_whenProdDisablesFlywayValidation_failsStartup() {
        ProductionReadinessGuard guard = guard(
                true,
                true,
                false,
                false,
                false,
                "validate"
        );

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.flyway.validate-on-migrate must be true in production.");
    }

    @Test
    void validate_whenProdUsesUnsafeSchemaManagement_failsStartup() {
        ProductionReadinessGuard guard = guard(
                true,
                true,
                true,
                true,
                true,
                "update"
        );

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.jpa.hibernate.ddl-auto must be validate in production.")
                .hasMessageContaining("spring.flyway.baseline-on-migrate must be false in production.")
                .hasMessageContaining("spring.flyway.out-of-order must be false in production.");
    }

    private ProductionReadinessGuard guard(boolean flywayEnabled,
                                           boolean flywayCleanDisabled,
                                           boolean flywayValidateOnMigrate,
                                           boolean flywayBaselineOnMigrate,
                                           boolean flywayOutOfOrder,
                                           String ddlAuto) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        return new ProductionReadinessGuard(
                environment,
                "prod-jwt-secret-long-enough-for-tests",
                false,
                "toss",
                "live_sk_test_secret",
                "https://shop.example.com",
                "https://shop.example.com",
                "jdbc:postgresql://db.example.internal:5432/alphashopper",
                ddlAuto,
                true,
                flywayEnabled,
                flywayCleanDisabled,
                flywayValidateOnMigrate,
                flywayBaselineOnMigrate,
                flywayOutOfOrder
        );
    }
}
