package com.curso.grossisports.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração tipada da sessão renovável (Aula 16).
 *
 * <p>O binding converte durações ISO-8601 (ex.: {@code PT30M}) para
 * {@link Duration}. A validação acontece no próprio construtor do
 * record: a aplicação falha na inicialização quando as durações são
 * inválidas, evitando uma sessão sem limite de inatividade ou com
 * expiração absoluta menor que a própria inatividade.</p>
 */
@ConfigurationProperties(prefix = "app.security.refresh-token")
public record RefreshTokenProperties(
    Duration idleTimeout,
    Duration absoluteExpiration) {

    public RefreshTokenProperties {
        if (idleTimeout == null
            || idleTimeout.isNegative()
            || idleTimeout.isZero()) {
            throw new IllegalArgumentException(
                "O limite de inatividade deve ser positivo");
        }

        if (absoluteExpiration == null
            || absoluteExpiration.isNegative()
            || absoluteExpiration.isZero()) {
            throw new IllegalArgumentException(
                "A validade absoluta deve ser positiva");
        }

        if (absoluteExpiration.compareTo(idleTimeout) <= 0) {
            throw new IllegalArgumentException(
                "A validade absoluta deve ser maior que o limite "
                    + "de inatividade");
        }
    }
}
