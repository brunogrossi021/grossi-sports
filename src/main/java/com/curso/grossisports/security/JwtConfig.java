package com.curso.grossisports.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.util.Base64;

/**
 * Infraestrutura criptográfica do JWT (Aula 13).
 *
 * <p>A chave HS256 vem de {@code JWT_SECRET} (arquivo {@code .env}, fora
 * do Git). O algoritmo é fixado em HS256 na emissão e na validação,
 * evitando aceitar um algoritmo inesperado indicado no header.</p>
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final int MINIMO_BYTES = 32;

    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        String valor = properties.secret();

        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET não foi definido. Gere uma chave Base64 "
                    + "de 256 bits e coloque no arquivo .env.");
        }

        byte[] chave;

        try {
            chave = Base64.getDecoder().decode(valor);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "JWT_SECRET deve estar codificada em Base64", exception);
        }

        if (chave.length < MINIMO_BYTES) {
            throw new IllegalStateException(
                "JWT_SECRET deve possuir pelo menos 32 bytes");
        }

        return new SecretKeySpec(chave, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
            .algorithm(MacAlgorithm.HS256)
            .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
        SecretKey jwtSecretKey,
        JwtProperties properties,
        Clock clock) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withSecretKey(jwtSecretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        JwtTimestampValidator tempo = new JwtTimestampValidator();
        tempo.setClock(clock);
        tempo.setAllowEmptyExpiryClaim(false);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            tempo,
            new JwtIssuerValidator(properties.issuer())));

        return decoder;
    }

    @Bean
    AuthenticationManager authenticationManager(
        AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
