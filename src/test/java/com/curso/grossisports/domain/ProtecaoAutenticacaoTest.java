package com.curso.grossisports.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras do contador de falhas de login (Aula 17).
 *
 * <p>O tempo é um parâmetro explícito dos métodos: a janela, o bloqueio e
 * a liberação são exercitados sem depender do relógio real nem de
 * esperas.</p>
 */
class ProtecaoAutenticacaoTest {

    private static final String CHAVE = "a".repeat(64);
    private static final int MAXIMO = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);
    private static final Instant BASE =
        Instant.parse("2026-11-15T12:00:00Z");

    @Test
    void deveRejeitarChaveQueNaoSejaHashSha256() {
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacao(null));
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacao("login-em-texto"));
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacao("A".repeat(64)));
        assertThrows(IllegalArgumentException.class,
            () -> new ProtecaoAutenticacao("a".repeat(63)));
    }

    @Test
    void deveIniciarSemFalhasNemBloqueio() {
        ProtecaoAutenticacao protecao = new ProtecaoAutenticacao(CHAVE);

        assertEquals(CHAVE, protecao.getChaveLogin());
        assertEquals(0, protecao.getFalhasConsecutivas());
        assertNull(protecao.getJanelaIniciadaEm());
        assertNull(protecao.getBloqueadaAte());
        assertFalse(protecao.estaBloqueada(BASE));
    }

    @Test
    void deveRegistrarPrimeiraFalhaAbrindoAJanela() {
        ProtecaoAutenticacao protecao = new ProtecaoAutenticacao(CHAVE);

        boolean ativou = protecao.registrarFalha(
            BASE, MAXIMO, JANELA, BLOQUEIO);

        assertFalse(ativou);
        assertEquals(1, protecao.getFalhasConsecutivas());
        assertEquals(BASE, protecao.getJanelaIniciadaEm());
        assertNull(protecao.getBloqueadaAte());
    }

    @Test
    void deveAtivarBloqueioExatamenteNaQuintaFalha() {
        ProtecaoAutenticacao protecao = new ProtecaoAutenticacao(CHAVE);

        for (int i = 1; i <= MAXIMO - 1; i++) {
            assertFalse(protecao.registrarFalha(
                BASE, MAXIMO, JANELA, BLOQUEIO));
        }

        assertTrue(protecao.registrarFalha(
            BASE, MAXIMO, JANELA, BLOQUEIO));

        assertEquals(MAXIMO, protecao.getFalhasConsecutivas());
        assertEquals(BASE.plus(BLOQUEIO), protecao.getBloqueadaAte());
        assertTrue(protecao.estaBloqueada(BASE));
    }

    @Test
    void deveLiberarNoInstanteExatoDoFimDoBloqueio() {
        ProtecaoAutenticacao protecao = new ProtecaoAutenticacao(CHAVE);

        for (int i = 0; i < MAXIMO; i++) {
            protecao.registrarFalha(BASE, MAXIMO, JANELA, BLOQUEIO);
        }

        Instant fim = BASE.plus(BLOQUEIO);

        assertTrue(protecao.estaBloqueada(fim.minusNanos(1)));
        assertFalse(protecao.estaBloqueada(fim));
        assertFalse(protecao.estaBloqueada(fim.plusSeconds(1)));
    }

    @Test
    void deveReiniciarContagemQuandoAJanelaExpira() {
        ProtecaoAutenticacao protecao = new ProtecaoAutenticacao(CHAVE);

        protecao.registrarFalha(BASE, MAXIMO, JANELA, BLOQUEIO);
        protecao.registrarFalha(BASE, MAXIMO, JANELA, BLOQUEIO);

        assertEquals(2, protecao.getFalhasConsecutivas());

        Instant depoisDaJanela = BASE.plus(JANELA).plusSeconds(1);

        boolean ativou = protecao.registrarFalha(
            depoisDaJanela, MAXIMO, JANELA, BLOQUEIO);

        assertFalse(ativou);
        assertEquals(1, protecao.getFalhasConsecutivas());
        assertEquals(depoisDaJanela, protecao.getJanelaIniciadaEm());
    }

    @Test
    void deveLimparFalhasJanelaEBloqueioAposSucesso() {
        ProtecaoAutenticacao protecao = new ProtecaoAutenticacao(CHAVE);

        for (int i = 0; i < MAXIMO; i++) {
            protecao.registrarFalha(BASE, MAXIMO, JANELA, BLOQUEIO);
        }

        protecao.registrarSucesso();

        assertEquals(0, protecao.getFalhasConsecutivas());
        assertNull(protecao.getJanelaIniciadaEm());
        assertNull(protecao.getBloqueadaAte());
        assertFalse(protecao.estaBloqueada(BASE));
    }

    @Test
    void deveRejeitarParametrosInvalidosAoRegistrarFalha() {
        ProtecaoAutenticacao protecao = new ProtecaoAutenticacao(CHAVE);

        assertThrows(NullPointerException.class,
            () -> protecao.registrarFalha(
                null, MAXIMO, JANELA, BLOQUEIO));
        assertThrows(NullPointerException.class,
            () -> protecao.registrarFalha(
                BASE, MAXIMO, null, BLOQUEIO));
        assertThrows(NullPointerException.class,
            () -> protecao.registrarFalha(
                BASE, MAXIMO, JANELA, null));
        assertThrows(IllegalArgumentException.class,
            () -> protecao.registrarFalha(
                BASE, 0, JANELA, BLOQUEIO));
    }
}
