package com.curso.grossisports.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração tipada da assinatura JWT (Aula 13).
 * O binding converte {@code PT30M} para {@link Duration}.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
    String issuer,
    Duration expiration,
    String secret) {
}
