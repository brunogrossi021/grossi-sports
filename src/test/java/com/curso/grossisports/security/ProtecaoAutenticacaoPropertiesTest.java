package com.curso.grossisports.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validação da política de proteção contra tentativas automatizadas
 * (Aula 17).
 *
 * <p>A validação vive no construtor do record, portanto falha na
 * inicialização em vez de deixar a aplicação sem limite de tentativas ou
 * com janelas e bloqueios inconsistentes.</p>
 */
class ProtecaoAutenticacaoPropertiesTest {

    private static final Duration JANELA = Duration.ofMinutes(15);
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    @Test
    void deveAceitarPoliticaValida() {
        ProtecaoAutenticacaoProperties properties =
            new ProtecaoAutenticacaoProperties(5, JANELA, BLOQUEIO);

        assertEquals(5, properties.maxFailedAttempts());
        assertEquals(JANELA, properties.failureWindow());
        assertEquals(BLOQUEIO, properties.lockDuration());
    }

    @Test
    void deveRejeitarMaximoDeFalhasNaoPositivo() {
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacaoProperties(0, JANELA, BLOQUEIO));
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacaoProperties(-1, JANELA, BLOQUEIO));
    }

    @Test
    void deveRejeitarJanelaAusenteZeroOuNegativa() {
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacaoProperties(5, null, BLOQUEIO));
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacaoProperties(
                5, Duration.ZERO, BLOQUEIO));
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacaoProperties(
                5, Duration.ofMinutes(-1), BLOQUEIO));
    }

    @Test
    void deveRejeitarBloqueioAusenteZeroOuNegativo() {
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacaoProperties(5, JANELA, null));
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacaoProperties(
                5, JANELA, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacaoProperties(
                5, JANELA, Duration.ofMinutes(-1)));
    }
}
