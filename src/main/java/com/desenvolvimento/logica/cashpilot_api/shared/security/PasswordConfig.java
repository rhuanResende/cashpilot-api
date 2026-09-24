package com.desenvolvimento.logica.cashpilot_api.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        PasswordEncoder argon2 = new Argon2PasswordEncoder(
                16,     // Tamanho do salt em bytes
                32,     // Tamanho do hash em bytes
                1,      // Paralelismo
                19456,  // Memória em KiB: 19 MiB
                2       // Iterações
        );

        return new DelegatingPasswordEncoder(
                "argon2",
                Map.of("argon2", argon2)
        );
    }
}
