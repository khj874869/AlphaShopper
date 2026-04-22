package com.webjpa.shopping.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogValuesTest {

    @Test
    void maskToken_keepsOnlyStableEdges() {
        assertThat(LogValues.maskToken("pay_abcdefghijklmnopqrstuvwxyz")).isEqualTo("pay_ab...wxyz");
    }

    @Test
    void maskToken_hidesShortValues() {
        assertThat(LogValues.maskToken("short")).isEqualTo("***");
    }

    @Test
    void maskEmail_hidesLocalPart() {
        assertThat(LogValues.maskEmail("buyer@example.com")).isEqualTo("b***@example.com");
    }

    @Test
    void safe_removesControlCharactersAndTruncates() {
        String value = "first\nsecond\tthird" + "x".repeat(200);

        assertThat(LogValues.safe(value))
                .doesNotContain("\n")
                .doesNotContain("\t")
                .endsWith("...");
    }
}
