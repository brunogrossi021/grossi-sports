package com.curso.grossisports.service;

import com.curso.grossisports.domain.ProtecaoAutenticacao;
import com.curso.grossisports.domain.TipoEventoSeguranca;
import com.curso.grossisports.repository.EventoSegurancaRepository;
import com.curso.grossisports.repository.ProtecaoAutenticacaoRepository;
import com.curso.grossisports.support.MutableClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concorrência do contador de falhas (Aula 17).
 *
 * <p>Duas falhas simultâneas não podem perder atualização: o
 * <em>upsert</em> nativo cria a primeira linha sem corrida e o
 * {@code SELECT ... FOR UPDATE} serializa as demais. A classe
 * <strong>não</strong> é {@code @Transactional}: cada chamada do serviço
 * precisa confirmar sua própria transação para que o bloqueio pessimista
 * seja realmente exercitado entre threads distintas. Não há
 * {@code sleep}: a sincronização usa barreiras e o tempo é controlado
 * pelo {@link MutableClock}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ProtecaoAutenticacaoServiceConcorrenciaTest {

    private static final Instant BASE =
        Instant.parse("2026-11-15T12:00:00Z");
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);
    private static final String LOGIN = "concorrencia.protecao.2026";

    @TestConfiguration
    static class RelogioControladoConfig {

        @Bean
        @Primary
        MutableClock relogioControlado() {
            return new MutableClock(BASE, ZoneOffset.UTC);
        }
    }

    @Autowired
    private ProtecaoAutenticacaoService service;

    @Autowired
    private ProtecaoAutenticacaoRepository protecaoRepository;

    @Autowired
    private EventoSegurancaRepository eventoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MutableClock relogio;

    @AfterEach
    void limpar() {
        String chave = service.hash(LOGIN);

        jdbcTemplate.update(
            "DELETE FROM evento_seguranca WHERE identificador_hash = ?",
            chave);
        jdbcTemplate.update(
            "DELETE FROM protecao_autenticacao WHERE chave_login = ?",
            chave);
    }

    @Test
    void deveContarFalhasSimultaneasSemPerderAtualizacao()
        throws Exception {

        relogio.definir(BASE);

        for (int i = 0; i < 3; i++) {
            service.registrarFalha(LOGIN, "127.0.0.1");
        }

        String chave = service.hash(LOGIN);

        assertEquals(
            3,
            protecaoRepository.findByChaveLogin(chave)
                .orElseThrow()
                .getFalhasConsecutivas());

        int participantes = 2;
        ExecutorService pool =
            Executors.newFixedThreadPool(participantes);
        CountDownLatch prontos = new CountDownLatch(participantes);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Void>> futuros = new ArrayList<>();

        try {
            for (int i = 0; i < participantes; i++) {
                Callable<Void> tarefa = () -> {
                    prontos.countDown();
                    largada.await();
                    service.registrarFalha(LOGIN, "127.0.0.1");
                    return null;
                };

                futuros.add(pool.submit(tarefa));
            }

            assertTrue(prontos.await(30, TimeUnit.SECONDS));
            largada.countDown();

            for (Future<Void> futuro : futuros) {
                futuro.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        ProtecaoAutenticacao protecao = protecaoRepository
            .findByChaveLogin(chave)
            .orElseThrow();

        assertEquals(5, protecao.getFalhasConsecutivas());
        assertEquals(BASE.plus(BLOQUEIO), protecao.getBloqueadaAte());
        assertTrue(protecao.estaBloqueada(BASE));

        assertEquals(
            5,
            eventoRepository.countByTipoAndIdentificadorHash(
                TipoEventoSeguranca.LOGIN_FALHA, chave));

        assertEquals(
            1,
            eventoRepository.countByTipoAndIdentificadorHash(
                TipoEventoSeguranca.BLOQUEIO_TEMPORARIO_ATIVADO, chave));
    }
}
