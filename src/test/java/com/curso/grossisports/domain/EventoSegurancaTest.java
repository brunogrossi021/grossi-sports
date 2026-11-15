package com.curso.grossisports.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Estrutura fechada dos eventos de segurança (Aula 17).
 *
 * <p>Os testes garantem que nenhum valor arbitrário entre no hash de
 * correlação ou no endereço de origem — a barreira contra
 * {@code log injection} e contra o registro acidental de dados
 * sensíveis.</p>
 */
class EventoSegurancaTest {

    private static final String HASH = "b".repeat(64);
    private static final Instant AGORA =
        Instant.parse("2026-11-15T12:00:00Z");

    @Test
    void deveRegistrarEventoCompleto() {
        EventoSeguranca evento = EventoSeguranca.registrar(
            TipoEventoSeguranca.LOGIN_FALHA,
            AGORA,
            1L,
            2L,
            HASH,
            "127.0.0.1");

        assertEquals(TipoEventoSeguranca.LOGIN_FALHA, evento.getTipo());
        assertEquals(AGORA, evento.getOcorridoEm());
        assertEquals(1L, evento.getAtorContaId());
        assertEquals(2L, evento.getAlvoContaId());
        assertEquals(HASH, evento.getIdentificadorHash());
        assertEquals("127.0.0.1", evento.getEnderecoOrigem());
    }

    @Test
    void deveAceitarCamposOpcionaisNulos() {
        EventoSeguranca evento = EventoSeguranca.registrar(
            TipoEventoSeguranca.REFRESH_FALHA, AGORA,
            null, null, null, null);

        assertNull(evento.getAtorContaId());
        assertNull(evento.getAlvoContaId());
        assertNull(evento.getIdentificadorHash());
        assertNull(evento.getEnderecoOrigem());
    }

    @Test
    void deveRejeitarTipoOuInstanteAusentes() {
        assertThrows(NullPointerException.class,
            () -> EventoSeguranca.registrar(
                null, AGORA, null, null, null, null));
        assertThrows(NullPointerException.class,
            () -> EventoSeguranca.registrar(
                TipoEventoSeguranca.LOGOUT, null, null, null, null, null));
    }

    @Test
    void deveRejeitarIdentificadorQueNaoSejaHashSha256() {
        assertThrows(IllegalArgumentException.class,
            () -> EventoSeguranca.registrar(
                TipoEventoSeguranca.LOGIN_FALHA, AGORA,
                null, null, "login-em-texto", null));
        assertThrows(IllegalArgumentException.class,
            () -> EventoSeguranca.registrar(
                TipoEventoSeguranca.LOGIN_FALHA, AGORA,
                null, null, "C".repeat(64), null));
        assertThrows(IllegalArgumentException.class,
            () -> EventoSeguranca.registrar(
                TipoEventoSeguranca.LOGIN_FALHA, AGORA,
                null, null, "c".repeat(63), null));
    }

    @Test
    void deveRejeitarOrigemComQuebraDeLinha() {
        assertThrows(IllegalArgumentException.class,
            () -> EventoSeguranca.registrar(
                TipoEventoSeguranca.LOGIN_FALHA, AGORA,
                null, null, null, "127.0.0.1\nforjado"));
        assertThrows(IllegalArgumentException.class,
            () -> EventoSeguranca.registrar(
                TipoEventoSeguranca.LOGIN_FALHA, AGORA,
                null, null, null, "127.0.0.1\rforjado"));
    }

    @Test
    void deveRejeitarOrigemMaiorQueQuarentaECincoCaracteres() {
        String longa = "1".repeat(46);

        assertThrows(IllegalArgumentException.class,
            () -> EventoSeguranca.registrar(
                TipoEventoSeguranca.LOGIN_FALHA, AGORA,
                null, null, null, longa));
    }

    @Test
    void deveRejeitarIdentificadoresDeContaNaoPositivos() {
        assertThrows(IllegalArgumentException.class,
            () -> EventoSeguranca.registrar(
                TipoEventoSeguranca.LOGIN_SUCESSO, AGORA,
                0L, null, null, null));
        assertThrows(IllegalArgumentException.class,
            () -> EventoSeguranca.registrar(
                TipoEventoSeguranca.LOGIN_SUCESSO, AGORA,
                null, -1L, null, null));
    }
}
