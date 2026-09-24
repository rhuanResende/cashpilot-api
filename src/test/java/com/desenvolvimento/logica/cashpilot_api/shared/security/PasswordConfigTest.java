package com.desenvolvimento.logica.cashpilot_api.shared.security;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordConfigTest {

    private final PasswordEncoder encoder =
            new PasswordConfig().passwordEncoder();

    @Test
    void shouldEncodeAndVerifyPassword() {
        String password = "Uma senha de teste longa!";
        String hash = encoder.encode(password);

        Assertions.assertThat(hash).startsWith("{argon2}$argon2id$");
        Assertions.assertThat(hash).isNotEqualTo(password);
        Assertions.assertThat(encoder.matches(password, hash)).isTrue();
        Assertions.assertThat(encoder.matches("Senha incorreta", hash)).isFalse();
    }

    @Test
    void shouldGenerateDifferentHashesForTheSamePassword() {
        String password = "Outra senha longa de teste!";

        String firstHash = encoder.encode(password);
        String secondHash = encoder.encode(password);

        Assertions.assertThat(firstHash).isNotEqualTo(secondHash);
        Assertions.assertThat(encoder.matches(password, firstHash)).isTrue();
        Assertions.assertThat(encoder.matches(password, secondHash)).isTrue();
    }
}
