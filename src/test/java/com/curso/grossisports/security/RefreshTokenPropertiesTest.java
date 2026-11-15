package com.curso.grossisports.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validação das durações da sessão renovável (Aula 16).
 *
 * <p>A validação vive no construtor do record, portanto é exercitada
 * aqui sem levantar o contexto do Spring: uma configuração inválida
 * falha de forma imediata e previsível, em vez de produzir uma sessão
 * sem limite de inatividade ou com validade absoluta inconsistente.</p>
 */
class RefreshTokenPropertiesTest {

    private static final Duration INATIVIDADE = Duration.ofMinutes(30);
    private static final Duration ABSOLUTA = Duration.ofHours(8);

    @Test
    void deveAceitarDuracoesValidas() {
        RefreshTokenProperties properties =
            new RefreshTokenProperties(INATIVIDADE, ABSOLUTA);

        assertEquals(INATIVIDADE, properties.idleTimeout());
        assertEquals(ABSOLUTA, properties.absoluteExpiration());
    }

    @Test
    void deveRejeitarInatividadeAusente() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RefreshTokenProperties(null, ABSOLUTA));
    }

    @Test
    void deveRejeitarInatividadeZero() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RefreshTokenProperties(
                Duration.ZERO, ABSOLUTA));
    }

    @Test
    void deveRejeitarInatividadeNegativa() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RefreshTokenProperties(
                Duration.ofMinutes(-1), ABSOLUTA));
    }

    @Test
    void deveRejeitarValidadeAbsolutaAusente() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RefreshTokenProperties(INATIVIDADE, null));
    }

    @Test
    void deveRejeitarValidadeAbsolutaZero() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RefreshTokenProperties(
                INATIVIDADE, Duration.ZERO));
    }

    @Test
    void deveRejeitarValidadeAbsolutaNegativa() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RefreshTokenProperties(
                INATIVIDADE, Duration.ofMinutes(-1)));
    }

    @Test
    void deveRejeitarAbsolutaIgualAInatividade() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RefreshTokenProperties(INATIVIDADE, INATIVIDADE));
    }

    @Test
    void deveRejeitarAbsolutaMenorQueInatividade() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RefreshTokenProperties(
                INATIVIDADE, Duration.ofMinutes(10)));
    }
}
