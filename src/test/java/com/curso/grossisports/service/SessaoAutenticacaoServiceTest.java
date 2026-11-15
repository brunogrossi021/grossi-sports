package com.curso.grossisports.service;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.MotivoRevogacaoRefreshToken;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.SessaoRefreshToken;
import com.curso.grossisports.exception.RefreshTokenInvalidoException;
import com.curso.grossisports.repository.ContaAcessoRepository;
import com.curso.grossisports.repository.PessoaRepository;
import com.curso.grossisports.repository.SessaoRefreshTokenRepository;
import com.curso.grossisports.security.TokensAutenticacao;
import com.curso.grossisports.support.MutableClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras de emissão e renovação da sessão renovável (Aula 16).
 *
 * <p>O relógio é controlado para exercitar os limites sem espera real.
 * A classe não é {@code @Transactional}: a renovação precisa confirmar
 * suas gravações para que a revogação sobreviva à exceção de token
 * inválido — exatamente o comportamento que se deseja comprovar.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SessaoAutenticacaoServiceTest {

    private static final Instant BASE =
        Instant.parse("2026-11-15T12:00:00Z");
    private static final String SENHA = "uma-senha-didatica-2026";
    private static final String ALGORITMO_HASH = "SHA-256";

    @TestConfiguration
    static class RelogioControladoConfig {

        @Bean
        @Primary
        MutableClock relogioControlado() {
            return new MutableClock(BASE, ZoneOffset.UTC);
        }
    }

    @Autowired
    private SessaoAutenticacaoService service;

    @Autowired
    private SessaoRefreshTokenRepository sessaoRepository;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MutableClock relogio;

    private final List<Long> contasCriadas = new ArrayList<>();
    private final List<Long> pessoasCriadas = new ArrayList<>();

    @BeforeEach
    void reiniciarRelogio() {
        relogio.definir(BASE);
    }

    @AfterEach
    void limpar() {
        for (Long contaId : contasCriadas) {
            jdbcTemplate.update(
                "DELETE FROM sessao_refresh_token "
                    + "WHERE conta_acesso_id = ?", contaId);
        }

        for (Long contaId : contasCriadas) {
            jdbcTemplate.update(
                "DELETE FROM conta_acesso_perfil "
                    + "WHERE conta_acesso_id = ?", contaId);
            jdbcTemplate.update(
                "DELETE FROM conta_acesso WHERE id = ?", contaId);
        }

        for (Long pessoaId : pessoasCriadas) {
            jdbcTemplate.update(
                "DELETE FROM pessoa WHERE id = ?", pessoaId);
        }

        contasCriadas.clear();
        pessoasCriadas.clear();
    }

    @Test
    void deveGerarRefreshTokenOpacoDe32Bytes() {
        ContaAcesso conta = criarConta();

        TokensAutenticacao tokens =
            service.iniciarSessao(autenticacao(conta));

        String refresh = tokens.refreshToken();

        assertNotNull(refresh);
        assertFalse(refresh.contains("="),
            "Base64 URL não deve ter padding");

        byte[] bytes = Base64.getUrlDecoder().decode(refresh);

        assertEquals(32, bytes.length);
        assertEquals(
            refresh,
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes));

        assertNotNull(tokens.acesso().valor());
        assertTrue(tokens.acesso().valor().split("\\.").length == 3);
    }

    @Test
    void deveArmazenarSomenteOHashDoRefreshToken() {
        ContaAcesso conta = criarConta();

        TokensAutenticacao tokens =
            service.iniciarSessao(autenticacao(conta));

        List<SessaoRefreshToken> sessoes =
            sessaoRepository.findByContaIdOrderByIdAsc(conta.getId());

        assertEquals(1, sessoes.size());

        SessaoRefreshToken sessao = sessoes.get(0);

        assertNotEquals(tokens.refreshToken(), sessao.getTokenHash());
        assertEquals(
            sha256Hex(tokens.refreshToken()), sessao.getTokenHash());
        assertEquals(64, sessao.getTokenHash().length());
        assertFalse(sessao.foiUtilizada());
        assertFalse(sessao.foiRevogada());
        assertEquals(
            tokens.refreshTokenExpiraEm(), sessao.getExpiraEm());
    }

    @Test
    void deveAdicionarClaimSidComAFamilia() {
        ContaAcesso conta = criarConta();

        TokensAutenticacao tokens =
            service.iniciarSessao(autenticacao(conta));

        Jwt jwt = jwtDecoder.decode(tokens.acesso().valor());

        String familia = sessaoRepository
            .findByContaIdOrderByIdAsc(conta.getId())
            .get(0)
            .getFamiliaId();

        assertEquals(familia, jwt.getClaimAsString("sid"));
        assertEquals(conta.getLogin(), jwt.getSubject());
        assertTrue(jwt.getClaimAsStringList("perfis")
            .contains("OPERADOR"));
    }

    @Test
    void deveRotacionarMantendoAFamiliaEAtualizandoAtividade() {
        ContaAcesso conta = criarConta();

        TokensAutenticacao primeiro =
            service.iniciarSessao(autenticacao(conta));

        relogio.avancar(Duration.ofMinutes(5));

        TokensAutenticacao segundo =
            service.renovar(primeiro.refreshToken());

        assertNotEquals(
            primeiro.refreshToken(), segundo.refreshToken());

        List<SessaoRefreshToken> sessoes =
            sessaoRepository.findByContaIdOrderByIdAsc(conta.getId());

        assertEquals(2, sessoes.size());

        SessaoRefreshToken antiga = sessoes.get(0);
        SessaoRefreshToken nova = sessoes.get(1);

        assertEquals(antiga.getFamiliaId(), nova.getFamiliaId());
        assertTrue(antiga.foiUtilizada());
        assertFalse(nova.foiUtilizada());
        assertFalse(nova.foiRevogada());

        // A validade absoluta é da família e não muda na rotação.
        assertEquals(antiga.getExpiraEm(), nova.getExpiraEm());
        assertEquals(BASE.plus(Duration.ofMinutes(5)),
            nova.getUltimaAtividadeEm());

        assertEquals(
            sha256Hex(segundo.refreshToken()), nova.getTokenHash());

        Jwt jwt = jwtDecoder.decode(segundo.acesso().valor());
        assertEquals(antiga.getFamiliaId(),
            jwt.getClaimAsString("sid"));
    }

    @Test
    void deveRejeitarSegundaUtilizacaoERevogarFamilia() {
        ContaAcesso conta = criarConta();

        TokensAutenticacao primeiro =
            service.iniciarSessao(autenticacao(conta));

        TokensAutenticacao segundo =
            service.renovar(primeiro.refreshToken());

        // Reapresenta a geração já trocada.
        assertThrows(
            RefreshTokenInvalidoException.class,
            () -> service.renovar(primeiro.refreshToken()));

        // A revogação sobreviveu à exceção (transação confirmada).
        List<SessaoRefreshToken> sessoes =
            sessaoRepository.findByContaIdOrderByIdAsc(conta.getId());

        assertEquals(2, sessoes.size());

        for (SessaoRefreshToken sessao : sessoes) {
            assertTrue(sessao.foiRevogada());
            assertEquals(
                MotivoRevogacaoRefreshToken.REUTILIZACAO,
                sessao.getMotivoRevogacao());
        }

        // O sucessor legítimo também deixa de renovar.
        assertThrows(
            RefreshTokenInvalidoException.class,
            () -> service.renovar(segundo.refreshToken()));
    }

    @Test
    void deveRejeitarRenovacaoAposInatividade() {
        ContaAcesso conta = criarConta();

        TokensAutenticacao tokens =
            service.iniciarSessao(autenticacao(conta));

        relogio.avancar(Duration.ofMinutes(31));

        assertThrows(
            RefreshTokenInvalidoException.class,
            () -> service.renovar(tokens.refreshToken()));

        SessaoRefreshToken sessao = sessaoRepository
            .findByContaIdOrderByIdAsc(conta.getId())
            .get(0);

        assertTrue(sessao.foiRevogada());
        assertEquals(
            MotivoRevogacaoRefreshToken.INATIVIDADE,
            sessao.getMotivoRevogacao());
    }

    @Test
    void deveRejeitarRenovacaoAposExpiracaoAbsoluta() {
        ContaAcesso conta = criarConta();

        TokensAutenticacao tokens =
            service.iniciarSessao(autenticacao(conta));

        relogio.avancar(Duration.ofHours(9));

        assertThrows(
            RefreshTokenInvalidoException.class,
            () -> service.renovar(tokens.refreshToken()));

        SessaoRefreshToken sessao = sessaoRepository
            .findByContaIdOrderByIdAsc(conta.getId())
            .get(0);

        assertTrue(sessao.foiRevogada());
        assertEquals(
            MotivoRevogacaoRefreshToken.EXPIRACAO_ABSOLUTA,
            sessao.getMotivoRevogacao());
    }

    @Test
    void deveRejeitarRenovacaoDeContaInativa() {
        ContaAcesso conta = criarConta();

        TokensAutenticacao tokens =
            service.iniciarSessao(autenticacao(conta));

        conta.bloquear();
        contaRepository.saveAndFlush(conta);

        assertThrows(
            RefreshTokenInvalidoException.class,
            () -> service.renovar(tokens.refreshToken()));

        SessaoRefreshToken sessao = sessaoRepository
            .findByContaIdOrderByIdAsc(conta.getId())
            .get(0);

        assertTrue(sessao.foiRevogada());
        assertEquals(
            MotivoRevogacaoRefreshToken.CONTA_INATIVA,
            sessao.getMotivoRevogacao());
    }

    @Test
    void deveRejeitarRefreshTokenDesconhecido() {
        assertThrows(
            RefreshTokenInvalidoException.class,
            () -> service.renovar("token-que-nao-existe"));
        assertThrows(
            RefreshTokenInvalidoException.class,
            () -> service.renovar("  "));
    }

    private ContaAcesso criarConta() {
        long semente = Math.abs(System.nanoTime());

        Pessoa pessoa = pessoaRepository.saveAndFlush(new Pessoa(
            "Pessoa Sessao",
            "sessao." + semente + "@example.com",
            gerarCpf(semente),
            LocalDate.of(2026, 1, 1)));

        ContaAcesso conta = contaRepository.saveAndFlush(
            new ContaAcesso(
                pessoa,
                "sessao." + semente,
                passwordEncoder.encode(SENHA)));

        contasCriadas.add(conta.getId());
        pessoasCriadas.add(pessoa.getId());

        return conta;
    }

    private Authentication autenticacao(ContaAcesso conta) {
        List<SimpleGrantedAuthority> autoridades = conta.getPerfis()
            .stream()
            .map(perfil ->
                new SimpleGrantedAuthority("ROLE_" + perfil.name()))
            .toList();

        return UsernamePasswordAuthenticationToken.authenticated(
            conta.getLogin(), null, autoridades);
    }

    private String sha256Hex(String valor) {
        try {
            byte[] resumo = MessageDigest
                .getInstance(ALGORITMO_HASH)
                .digest(valor.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
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
