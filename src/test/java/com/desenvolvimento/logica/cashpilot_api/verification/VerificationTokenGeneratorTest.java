package com.desenvolvimento.logica.cashpilot_api.verification;

import com.desenvolvimento.logica.cashpilot_api.verification.config.VerificationTokenGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VerificationTokenGeneratorTest {

    private final VerificationTokenGenerator generator =
            new VerificationTokenGenerator();

    @Test
    void shouldGenerateUrlSafeTokens() {
        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).matches("[A-Za-z0-9_-]{43}");
        assertThat(second).matches("[A-Za-z0-9_-]{43}");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldGenerateDeterministicHash() {
        String token = generator.generate();

        String firstHash = generator.hash(token);
        String secondHash = generator.hash(token);

        assertThat(firstHash).matches("[0-9a-f]{64}");
        assertThat(firstHash).isEqualTo(secondHash);
        assertThat(firstHash).isNotEqualTo(token);
    }

    @Test
    void shouldRejectInvalidTokenFormat() {
        assertThatThrownBy(() -> generator.hash("token-invalido"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> generator.hash(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
