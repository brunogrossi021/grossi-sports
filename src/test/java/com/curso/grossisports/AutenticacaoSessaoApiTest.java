package com.curso.grossisports;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.MotivoRevogacaoRefreshToken;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.SessaoRefreshToken;
import com.curso.grossisports.repository.ContaAcessoRepository;
import com.curso.grossisports.repository.PessoaRepository;
import com.curso.grossisports.repository.SessaoRefreshTokenRepository;
import com.curso.grossisports.support.MutableClock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida HTTP dos tokens (Aula 16): login, renovação, logout
 * local, logout global e os limites da sessão.
 *
 * <p>A classe <strong>não</strong> é {@code @Transactional}: a renovação
 * e o logout precisam confirmar suas gravações para que a revogação seja
 * observada por consultas novas — exatamente o comportamento que se
 * deseja comprovar. O relógio é controlado para exercitar inatividade e
 * expiração absoluta sem espera real.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutenticacaoSessaoApiTest {

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
    private MockMvc mockMvc;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private SessaoRefreshTokenRepository sessaoRepository;

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

    // ---------------------------------------------------------------
    // Login
    // ---------------------------------------------------------------

    @Test
    void deveEmitirOsDoisTokensNoLogin() throws Exception {
        ContaAcesso conta = criarConta();

        mockMvc.perform(login(conta.getLogin(), SENHA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(1800))
            .andExpect(jsonPath("$.expiresAt")
                .value("2026-11-15T12:30:00Z"))
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshTokenExpiresAt")
                .value("2026-11-15T20:00:00Z"));

        List<SessaoRefreshToken> sessoes = sessoes(conta);

        assertEquals(1, sessoes.size());

        SessaoRefreshToken sessao = sessoes.get(0);

        assertEquals(64, sessao.getTokenHash().length());
        assertFalse(sessao.foiUtilizada());
        assertFalse(sessao.foiRevogada());
        assertEquals(BASE, sessao.getCriadaEm());
        assertEquals(BASE, sessao.getUltimaAtividadeEm());
        assertEquals(BASE.plus(Duration.ofHours(8)),
            sessao.getExpiraEm());
    }

    @Test
    void deveGuardarApenasOHashDoRefreshTokenNoLogin()
        throws Exception {

        ContaAcesso conta = criarConta();

        String corpo = executarLogin(conta.getLogin(), SENHA);
        String refreshToken = JsonPath.read(corpo, "$.refreshToken");

        SessaoRefreshToken sessao = sessoes(conta).get(0);

        assertNotEquals(refreshToken, sessao.getTokenHash());
        assertEquals(sha256Hex(refreshToken), sessao.getTokenHash());
    }

    // ---------------------------------------------------------------
    // Renovação e rotação
    // ---------------------------------------------------------------

    @Test
    void deveRenovarRotacionandoAMesmaFamilia() throws Exception {
        ContaAcesso conta = criarConta();

        String primeiro = executarLogin(conta.getLogin(), SENHA);
        String refreshAntigo = JsonPath.read(primeiro, "$.refreshToken");

        relogio.avancar(Duration.ofMinutes(5));

        String renovado = mockMvc.perform(refresh(refreshAntigo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String refreshNovo = JsonPath.read(renovado, "$.refreshToken");

        assertNotEquals(refreshAntigo, refreshNovo);

        List<SessaoRefreshToken> sessoes = sessoes(conta);

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
        assertEquals(sha256Hex(refreshNovo), nova.getTokenHash());

        Jwt jwt = jwtDecoder.decode(
            JsonPath.read(renovado, "$.accessToken"));
        assertEquals(nova.getFamiliaId(), jwt.getClaimAsString("sid"));
    }

    // ---------------------------------------------------------------
    // Reutilização
    // ---------------------------------------------------------------

    @Test
    void deveRejeitarReutilizacaoERevogarFamilia() throws Exception {
        ContaAcesso conta = criarConta();

        String primeiro = executarLogin(conta.getLogin(), SENHA);
        String refreshAntigo = JsonPath.read(primeiro, "$.refreshToken");

        String renovado = mockMvc.perform(refresh(refreshAntigo))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String refreshNovo = JsonPath.read(renovado, "$.refreshToken");

        // Reapresenta a geração já trocada: 401 genérico.
        mockMvc.perform(refresh(refreshAntigo))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // A revogação sobreviveu à exceção (transação confirmada).
        List<SessaoRefreshToken> sessoes = sessoes(conta);

        assertEquals(2, sessoes.size());

        for (SessaoRefreshToken sessao : sessoes) {
            assertTrue(sessao.foiRevogada());
            assertEquals(
                MotivoRevogacaoRefreshToken.REUTILIZACAO,
                sessao.getMotivoRevogacao());
        }

        // O sucessor legítimo também deixa de renovar.
        mockMvc.perform(refresh(refreshNovo))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // Logout local e global
    // ---------------------------------------------------------------

    @Test
    void deveEncerrarSessaoLocalNoLogout() throws Exception {
        ContaAcesso conta = criarConta();

        String corpo = executarLogin(conta.getLogin(), SENHA);
        String refreshToken = JsonPath.read(corpo, "$.refreshToken");

        mockMvc.perform(logout(refreshToken))
            .andExpect(status().isNoContent());

        SessaoRefreshToken sessao = sessoes(conta).get(0);

        assertTrue(sessao.foiRevogada());
        assertEquals(
            MotivoRevogacaoRefreshToken.LOGOUT,
            sessao.getMotivoRevogacao());

        mockMvc.perform(refresh(refreshToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveSerIdempotenteNoLogoutDeTokenDesconhecido()
        throws Exception {

        mockMvc.perform(logout("token-que-nao-existe"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deveEncerrarTodasAsSessoesNoLogoutGlobal() throws Exception {
        ContaAcesso conta = criarConta();

        String primeira = executarLogin(conta.getLogin(), SENHA);
        String segunda = executarLogin(conta.getLogin(), SENHA);

        String refreshA = JsonPath.read(primeira, "$.refreshToken");
        String refreshB = JsonPath.read(segunda, "$.refreshToken");
        String acessoA = JsonPath.read(primeira, "$.accessToken");

        assertEquals(2, sessoes(conta).size());
        assertNotEquals(refreshA, refreshB);

        mockMvc.perform(logoutTodos(acessoA))
            .andExpect(status().isNoContent());

        List<SessaoRefreshToken> sessoes = sessoes(conta);

        assertEquals(2, sessoes.size());

        for (SessaoRefreshToken sessao : sessoes) {
            assertTrue(sessao.foiRevogada());
            assertEquals(
                MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL,
                sessao.getMotivoRevogacao());
        }

        mockMvc.perform(refresh(refreshA))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(refresh(refreshB))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveExigirBearerNoLogoutGlobal() throws Exception {
        mockMvc.perform(post("/api/autenticacao/logout-todos"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // Limites da sessão
    // ---------------------------------------------------------------

    @Test
    void deveRejeitarRenovacaoDeContaInativa() throws Exception {
        ContaAcesso conta = criarConta();

        String corpo = executarLogin(conta.getLogin(), SENHA);
        String refreshToken = JsonPath.read(corpo, "$.refreshToken");

        conta.bloquear();
        contaRepository.saveAndFlush(conta);

        mockMvc.perform(refresh(refreshToken))
            .andExpect(status().isUnauthorized());

        SessaoRefreshToken sessao = sessoes(conta).get(0);

        assertTrue(sessao.foiRevogada());
        assertEquals(
            MotivoRevogacaoRefreshToken.CONTA_INATIVA,
            sessao.getMotivoRevogacao());
    }

    @Test
    void deveRejeitarRenovacaoAposInatividade() throws Exception {
        ContaAcesso conta = criarConta();

        String corpo = executarLogin(conta.getLogin(), SENHA);
        String refreshToken = JsonPath.read(corpo, "$.refreshToken");

        relogio.avancar(Duration.ofMinutes(31));

        mockMvc.perform(refresh(refreshToken))
            .andExpect(status().isUnauthorized());

        SessaoRefreshToken sessao = sessoes(conta).get(0);

        assertTrue(sessao.foiRevogada());
        assertEquals(
            MotivoRevogacaoRefreshToken.INATIVIDADE,
            sessao.getMotivoRevogacao());
    }

    @Test
    void deveRejeitarRenovacaoAposExpiracaoAbsoluta() throws Exception {
        ContaAcesso conta = criarConta();

        String corpo = executarLogin(conta.getLogin(), SENHA);
        String refreshToken = JsonPath.read(corpo, "$.refreshToken");

        relogio.avancar(Duration.ofHours(9));

        mockMvc.perform(refresh(refreshToken))
            .andExpect(status().isUnauthorized());

        SessaoRefreshToken sessao = sessoes(conta).get(0);

        assertTrue(sessao.foiRevogada());
        assertEquals(
            MotivoRevogacaoRefreshToken.EXPIRACAO_ABSOLUTA,
            sessao.getMotivoRevogacao());
    }

    // ---------------------------------------------------------------
    // Logout não invalida o access token já emitido
    // ---------------------------------------------------------------

    @Test
    void deveManterAccessTokenValidoAposLogout() throws Exception {
        ContaAcesso conta = criarConta();

        String corpo = executarLogin(conta.getLogin(), SENHA);
        String refreshToken = JsonPath.read(corpo, "$.refreshToken");
        String acesso = JsonPath.read(corpo, "$.accessToken");

        mockMvc.perform(logout(refreshToken))
            .andExpect(status().isNoContent());

        // O refresh foi revogado, mas o access token (JWT autocontido)
        // continua válido até expirar, dentro dos 30 minutos originais.
        mockMvc.perform(get("/api/produtos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + acesso))
            .andExpect(status().isOk());

        Jwt jwt = jwtDecoder.decode(acesso);
        assertEquals(BASE.plus(Duration.ofMinutes(30)), jwt.getExpiresAt());
    }

    @Test
    void deveRejeitarRefreshComCorpoInvalido() throws Exception {
        mockMvc.perform(post("/api/autenticacao/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/autenticacao/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"  \"}"))
            .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // Apoio
    // ---------------------------------------------------------------

    private MockHttpServletRequestBuilder login(
        String login, String senha) {

        return post("/api/autenticacao/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "login": "%s",
                  "senha": "%s"
                }
                """.formatted(login, senha));
    }

    private MockHttpServletRequestBuilder refresh(String refreshToken) {
        return post("/api/autenticacao/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\": \"%s\"}"
                .formatted(refreshToken));
    }

    private MockHttpServletRequestBuilder logout(String refreshToken) {
        return post("/api/autenticacao/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\": \"%s\"}"
                .formatted(refreshToken));
    }

    private MockHttpServletRequestBuilder logoutTodos(String accessToken) {
        return post("/api/autenticacao/logout-todos")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private String executarLogin(String login, String senha)
        throws Exception {

        return mockMvc.perform(login(login, senha))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private List<SessaoRefreshToken> sessoes(ContaAcesso conta) {
        return sessaoRepository.findByContaIdOrderByIdAsc(conta.getId());
    }

    private ContaAcesso criarConta() {
        long semente = Math.abs(System.nanoTime());

        Pessoa pessoa = pessoaRepository.saveAndFlush(new Pessoa(
            "Pessoa Sessao API",
            "sessao.api." + semente + "@example.com",
            gerarCpf(semente),
            LocalDate.of(2026, 1, 1)));

        ContaAcesso conta = contaRepository.saveAndFlush(
            new ContaAcesso(
                pessoa,
                "sessao.api." + semente,
                passwordEncoder.encode(SENHA)));

        contasCriadas.add(conta.getId());
        pessoasCriadas.add(pessoa.getId());

        return conta;
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
