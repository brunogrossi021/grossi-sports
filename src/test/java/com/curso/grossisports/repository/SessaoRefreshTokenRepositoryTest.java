package com.curso.grossisports.repository;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.MotivoRevogacaoRefreshToken;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.SessaoRefreshToken;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercita o repositório de sessões de refresh token (Aula 16).
 *
 * <p>Confirma que a busca por hash traz conta e pessoa na mesma
 * consulta (join fetch), que as revogações atingem apenas as gerações
 * ainda ativas do escopo pedido e que a consulta de atualização realmente
 * bloqueia a linha contra outra transação concorrente.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SessaoRefreshTokenRepositoryTest {

    @Autowired
    private SessaoRefreshTokenRepository repository;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional
    void deveBuscarSessaoPorHashCarregandoContaEPessoa() {
        ContaAcesso conta = criarConta("busca");
        String hash = hash();

        repository.saveAndFlush(
            novaSessao(conta, "familia-busca", hash));

        entityManager.clear();

        Optional<SessaoRefreshToken> encontrada =
            repository.buscarPorHashParaAtualizacao(hash);

        assertTrue(encontrada.isPresent());
        assertEquals("familia-busca",
            encontrada.get().getFamiliaId());
        assertEquals(hash, encontrada.get().getTokenHash());
        assertEquals(conta.getId(),
            encontrada.get().getConta().getId());
        assertEquals("Pessoa busca",
            encontrada.get().getConta().getPessoa().getNome());
    }

    @Test
    @Transactional
    void deveRetornarVazioParaHashDesconhecido() {
        assertTrue(repository
            .buscarPorHashParaAtualizacao(hash())
            .isEmpty());
    }

    @Test
    @Transactional
    void deveRevogarSomenteGeracoesAtivasDaFamilia() {
        ContaAcesso conta = criarConta("familia");

        SessaoRefreshToken ativa1 = repository.saveAndFlush(
            novaSessao(conta, "familia-x", hash()));
        SessaoRefreshToken ativa2 = repository.saveAndFlush(
            novaSessao(conta, "familia-x", hash()));

        SessaoRefreshToken jaRevogada = repository.saveAndFlush(
            novaSessao(conta, "familia-x", hash()));
        jaRevogada.revogar(
            Instant.now(), MotivoRevogacaoRefreshToken.LOGOUT);
        repository.saveAndFlush(jaRevogada);

        SessaoRefreshToken outraFamilia = repository.saveAndFlush(
            novaSessao(conta, "familia-y", hash()));

        int afetadas = repository.revogarFamilia(
            "familia-x", Instant.now(),
            MotivoRevogacaoRefreshToken.REUTILIZACAO);

        assertEquals(2, afetadas);

        entityManager.clear();

        SessaoRefreshToken r1 = repository
            .findById(ativa1.getId()).orElseThrow();
        SessaoRefreshToken r2 = repository
            .findById(ativa2.getId()).orElseThrow();
        SessaoRefreshToken r3 = repository
            .findById(jaRevogada.getId()).orElseThrow();
        SessaoRefreshToken r4 = repository
            .findById(outraFamilia.getId()).orElseThrow();

        assertTrue(r1.foiRevogada());
        assertTrue(r2.foiRevogada());
        assertEquals(MotivoRevogacaoRefreshToken.REUTILIZACAO,
            r1.getMotivoRevogacao());
        assertEquals(MotivoRevogacaoRefreshToken.REUTILIZACAO,
            r2.getMotivoRevogacao());

        // A revogação anterior não é sobrescrita.
        assertTrue(r3.foiRevogada());
        assertEquals(MotivoRevogacaoRefreshToken.LOGOUT,
            r3.getMotivoRevogacao());

        assertFalse(r4.foiRevogada());
    }

    @Test
    @Transactional
    void deveRevogarTodasAsGeracoesAtivasDaConta() {
        ContaAcesso alvo = criarConta("global");
        ContaAcesso outra = criarConta("outra");

        SessaoRefreshToken s1 = repository.saveAndFlush(
            novaSessao(alvo, "f1", hash()));
        SessaoRefreshToken s2 = repository.saveAndFlush(
            novaSessao(alvo, "f2", hash()));
        SessaoRefreshToken s3 = repository.saveAndFlush(
            novaSessao(outra, "f3", hash()));

        int afetadas = repository.revogarTodasDaConta(
            alvo.getId(), Instant.now(),
            MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL);

        assertEquals(2, afetadas);

        entityManager.clear();

        SessaoRefreshToken r1 = repository
            .findById(s1.getId()).orElseThrow();
        SessaoRefreshToken r2 = repository
            .findById(s2.getId()).orElseThrow();
        SessaoRefreshToken r3 = repository
            .findById(s3.getId()).orElseThrow();

        assertTrue(r1.foiRevogada());
        assertTrue(r2.foiRevogada());
        assertEquals(MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL,
            r1.getMotivoRevogacao());
        assertFalse(r3.foiRevogada());
    }

    /**
     * Sem {@code @Transactional}: o dado precisa estar confirmado para a
     * segunda transação enxergá-lo. A primeira transação segura o
     * bloqueio pessimista por 1,5 s; a segunda só pode prosseguir quando
     * a primeira terminar, o que comprova o {@code SELECT ... FOR UPDATE}.
     */
    @Test
    void deveBloquearAtualizacaoConcorrenteDaMesmaSessao()
        throws Exception {

        TransactionTemplate transactionTemplate =
            new TransactionTemplate(transactionManager);

        String hash = hash();
        AtomicReference<Long> sessaoId = new AtomicReference<>();
        AtomicReference<Long> contaId = new AtomicReference<>();
        AtomicReference<Long> pessoaId = new AtomicReference<>();

        transactionTemplate.executeWithoutResult(status -> {
            ContaAcesso conta = criarConta("lock");
            contaId.set(conta.getId());
            pessoaId.set(conta.getPessoa().getId());

            SessaoRefreshToken sessao = repository.saveAndFlush(
                novaSessao(conta, "familia-lock", hash));
            sessaoId.set(sessao.getId());
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch bloqueioObtido = new CountDownLatch(1);
        CountDownLatch segundoIniciado = new CountDownLatch(1);
        AtomicLong esperaMs = new AtomicLong(-1);

        try {
            Future<?> primeiro = executor.submit(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    repository
                        .buscarPorHashParaAtualizacao(hash);
                    bloqueioObtido.countDown();

                    try {
                        segundoIniciado.await(10, TimeUnit.SECONDS);
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));

            Future<?> segundo = executor.submit(() -> {
                try {
                    bloqueioObtido.await(10, TimeUnit.SECONDS);
                    segundoIniciado.countDown();

                    long inicio = System.nanoTime();

                    transactionTemplate.executeWithoutResult(status ->
                        repository
                            .buscarPorHashParaAtualizacao(hash));

                    esperaMs.set(
                        (System.nanoTime() - inicio) / 1_000_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            primeiro.get(30, TimeUnit.SECONDS);
            segundo.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();

            jdbcTemplate.update(
                "DELETE FROM sessao_refresh_token WHERE id = ?",
                sessaoId.get());
            jdbcTemplate.update(
                "DELETE FROM conta_acesso WHERE id = ?", contaId.get());
            jdbcTemplate.update(
                "DELETE FROM pessoa WHERE id = ?", pessoaId.get());
        }

        assertTrue(
            esperaMs.get() >= 1000,
            "A segunda transação deveria aguardar o bloqueio pessimista, "
                + "mas concluiu em " + esperaMs.get() + " ms");
    }

    private ContaAcesso criarConta(String identificador) {
        long semente = Math.abs(System.nanoTime());

        Pessoa pessoa = pessoaRepository.saveAndFlush(new Pessoa(
            "Pessoa " + identificador,
            identificador + "." + semente + "@example.com",
            gerarCpf(semente),
            LocalDate.of(2026, 1, 1)));

        return contaRepository.saveAndFlush(new ContaAcesso(
            pessoa,
            identificador + "." + semente,
            "{noop}segredo-local"));
    }

    private SessaoRefreshToken novaSessao(
        ContaAcesso conta, String familiaId, String hash) {

        Instant agora = Instant.now();

        return new SessaoRefreshToken(
            conta,
            familiaId,
            hash,
            agora,
            agora,
            agora.plus(Duration.ofHours(8)));
    }

    private String hash() {
        return (UUID.randomUUID().toString()
            + UUID.randomUUID().toString())
            .replace("-", "");
    }

    private String gerarCpf(long semente) {
        String base = "%09d".formatted(
            Math.floorMod(semente, 1_000_000_000L));

        if (digitosRepetidos(base)) {
            base = "123456789";
        }

        int primeiro = digitoCpf(base, 10);

        return base + primeiro + digitoCpf(base + primeiro, 11);
    }

    private int digitoCpf(String base, int peso) {
        int soma = 0;

        for (char c : base.toCharArray()) {
            soma += Character.getNumericValue(c) * peso--;
        }

        int resto = soma % 11;

        return resto < 2 ? 0 : 11 - resto;
    }

    private boolean digitosRepetidos(String valor) {
        for (int i = 1; i < valor.length(); i++) {
            if (valor.charAt(i) != valor.charAt(0)) {
                return false;
            }
        }

        return true;
    }
}
