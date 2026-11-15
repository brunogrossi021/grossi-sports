package com.curso.grossisports;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.ProtecaoAutenticacao;
import com.curso.grossisports.domain.TipoEventoSeguranca;
import com.curso.grossisports.repository.ContaAcessoRepository;
import com.curso.grossisports.repository.EventoSegurancaRepository;
import com.curso.grossisports.repository.PessoaRepository;
import com.curso.grossisports.repository.ProtecaoAutenticacaoRepository;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proteção contra tentativas automatizadas de login no nível HTTP
 * (Aula 17).
 *
 * <p>A classe <strong>não</strong> é {@code @Transactional}: o contador
 * precisa confirmar a gravação antes de a resposta 401 ser devolvida —
 * exatamente o comportamento que se quer comprovar. O relógio é
 * controlado para testar a liberação no fim do bloqueio sem espera real
 * e a limpeza é feita por {@link JdbcTemplate}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProtecaoAutenticacaoApiTest {

    private static final Instant BASE =
        Instant.parse("2026-11-15T12:00:00Z");
    private static final String SENHA = "uma-senha-didatica-2026";
    private static final String ALGORITMO_HASH = "SHA-256";
    private static final Duration JANELA = Duration.ofMinutes(15);
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

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
    private ProtecaoAutenticacaoRepository protecaoRepository;

    @Autowired
    private EventoSegurancaRepository eventoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MutableClock relogio;

    private final List<Long> contasCriadas = new ArrayList<>();
    private final List<Long> pessoasCriadas = new ArrayList<>();
    private final List<String> chavesCriadas = new ArrayList<>();

    @BeforeEach
    void reiniciarRelogio() {
        relogio.definir(BASE);
    }

    @AfterEach
    void limpar() {
        for (String chave : chavesCriadas) {
            jdbcTemplate.update(
                "DELETE FROM evento_seguranca "
                    + "WHERE identificador_hash = ?", chave);
            jdbcTemplate.update(
                "DELETE FROM protecao_autenticacao "
                    + "WHERE chave_login = ?", chave);
        }

        for (Long contaId : contasCriadas) {
            jdbcTemplate.update(
                "DELETE FROM sessao_refresh_token "
                    + "WHERE conta_acesso_id = ?", contaId);
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
        chavesCriadas.clear();
    }

    // ---------------------------------------------------------------
    // Bloqueio na quinta falha
    // ---------------------------------------------------------------

    @Test
    void deveBloquearNaQuintaFalhaEGravarEventos() throws Exception {
        ContaAcesso conta = criarConta("protecao.bloqueio");
        String chave = registrarChave(conta.getLogin());

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(login(conta.getLogin(), "senha-errada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                    .value("Login ou senha inválidos"));
        }

        ProtecaoAutenticacao protecao = protecaoRepository
            .findByChaveLogin(chave)
            .orElseThrow();

        assertEquals(5, protecao.getFalhasConsecutivas());
        assertEquals(BASE.plus(BLOQUEIO), protecao.getBloqueadaAte());
        assertTrue(protecao.estaBloqueada(BASE));

        assertEquals(5, eventoRepository
            .countByTipoAndIdentificadorHash(
                TipoEventoSeguranca.LOGIN_FALHA, chave));
        assertEquals(1, eventoRepository
            .countByTipoAndIdentificadorHash(
                TipoEventoSeguranca.BLOQUEIO_TEMPORARIO_ATIVADO, chave));
    }

    @Test
    void deveRecusarSenhaCorretaDuranteOBloqueio() throws Exception {
        ContaAcesso conta = criarConta("protecao.senha.correta");
        String chave = registrarChave(conta.getLogin());

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(login(conta.getLogin(), "senha-errada"))
                .andExpect(status().isUnauthorized());
        }

        // Senha correta, mas bloqueio ativo: recusa antes de BCrypt.
        mockMvc.perform(login(conta.getLogin(), SENHA))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message")
                .value("Login ou senha inválidos"));

        assertEquals(0, contarSessoes(conta.getId()));
        assertEquals(1, eventoRepository
            .countByTipoAndIdentificadorHash(
                TipoEventoSeguranca.LOGIN_RECUSADO_BLOQUEIO, chave));
    }

    @Test
    void deveLiberarExatamenteNoFimDoBloqueio() throws Exception {
        ContaAcesso conta = criarConta("protecao.liberacao");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(login(conta.getLogin(), "senha-errada"))
                .andExpect(status().isUnauthorized());
        }

        relogio.avancar(BLOQUEIO.minusNanos(1));

        mockMvc.perform(login(conta.getLogin(), SENHA))
            .andExpect(status().isUnauthorized());

        relogio.avancar(Duration.ofNanos(1));

        mockMvc.perform(login(conta.getLogin(), SENHA))
            .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------
    // Normalização e resposta genérica
    // ---------------------------------------------------------------

    @Test
    void deveAgruparVariacoesDeCaixaEEspacos() throws Exception {
        ContaAcesso conta = criarConta("protecao.normaliza");
        String chave = registrarChave(conta.getLogin());

        mockMvc.perform(login("PROTECAO.NORMALIZA", "senha-errada"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(login(" Protecao.Normaliza ", "senha-errada"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(login("protecao.normaliza", "senha-errada"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(login("PROTECAO.NORMALIZA ", "senha-errada"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(login(" Protecao.normaliza", "senha-errada"))
            .andExpect(status().isUnauthorized());

        ProtecaoAutenticacao protecao = protecaoRepository
            .findByChaveLogin(chave)
            .orElseThrow();

        assertEquals(5, protecao.getFalhasConsecutivas());
        assertTrue(protecao.estaBloqueada(BASE));
    }

    @Test
    void deveResponderDeFormaGenericaParaContaExistenteEInexistente()
        throws Exception {

        ContaAcesso conta = criarConta("protecao.generica");
        registrarChave("login.que.nao.existe");

        String existente = mockMvc
            .perform(login(conta.getLogin(), "senha-errada"))
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();

        String inexistente = mockMvc
            .perform(login("login.que.nao.existe", "senha-errada"))
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();

        String mensagemExistente =
            JsonPath.read(existente, "$.message");
        String mensagemInexistente =
            JsonPath.read(inexistente, "$.message");

        assertEquals(mensagemExistente, mensagemInexistente);
        assertEquals("Login ou senha inválidos", mensagemExistente);
        assertEquals(
            401,
            ((Number) JsonPath.read(existente, "$.status")).intValue());
    }

    // ---------------------------------------------------------------
    // Persistência e limpeza
    // ---------------------------------------------------------------

    @Test
    void devePersistirContadorAposResposta401() throws Exception {
        ContaAcesso conta = criarConta("protecao.persiste");
        String chave = registrarChave(conta.getLogin());

        mockMvc.perform(login(conta.getLogin(), "senha-errada"))
            .andExpect(status().isUnauthorized());

        ProtecaoAutenticacao protecao = protecaoRepository
            .findByChaveLogin(chave)
            .orElseThrow();

        assertEquals(1, protecao.getFalhasConsecutivas());
        assertEquals(BASE, protecao.getJanelaIniciadaEm());
        assertNull(protecao.getBloqueadaAte());
    }

    @Test
    void deveLimparFalhasAposLoginValido() throws Exception {
        ContaAcesso conta = criarConta("protecao.limpa");
        String chave = registrarChave(conta.getLogin());

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(login(conta.getLogin(), "senha-errada"))
                .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(login(conta.getLogin(), SENHA))
            .andExpect(status().isOk());

        ProtecaoAutenticacao protecao = protecaoRepository
            .findByChaveLogin(chave)
            .orElseThrow();

        assertEquals(0, protecao.getFalhasConsecutivas());
        assertNull(protecao.getJanelaIniciadaEm());
        assertNull(protecao.getBloqueadaAte());

        assertEquals(1, eventoRepository
            .countByTipoAndIdentificadorHash(
                TipoEventoSeguranca.LOGIN_SUCESSO, chave));
    }

    @Test
    void deveReiniciarJanelaQuandoElaExpira() throws Exception {
        ContaAcesso conta = criarConta("protecao.janela");
        String chave = registrarChave(conta.getLogin());

        mockMvc.perform(login(conta.getLogin(), "senha-errada"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(login(conta.getLogin(), "senha-errada"))
            .andExpect(status().isUnauthorized());

        relogio.avancar(JANELA.plusSeconds(1));

        mockMvc.perform(login(conta.getLogin(), "senha-errada"))
            .andExpect(status().isUnauthorized());

        ProtecaoAutenticacao protecao = protecaoRepository
            .findByChaveLogin(chave)
            .orElseThrow();

        assertEquals(1, protecao.getFalhasConsecutivas());
        assertEquals(
            BASE.plus(JANELA).plusSeconds(1),
            protecao.getJanelaIniciadaEm());
    }

    // ---------------------------------------------------------------
    // Minimização de dados
    // ---------------------------------------------------------------

    @Test
    void deveArmazenarApenasOHashDoLoginNormalizado() throws Exception {
        ContaAcesso conta = criarConta("protecao.hash");
        String chave = registrarChave(conta.getLogin());

        mockMvc.perform(login(conta.getLogin(), "senha-errada"))
            .andExpect(status().isUnauthorized());

        String esperado = sha256Hex(
            conta.getLogin().trim().toLowerCase(Locale.ROOT));

        assertEquals(esperado, chave);

        String armazenado = jdbcTemplate.queryForObject(
            "SELECT chave_login FROM protecao_autenticacao "
                + "WHERE chave_login = ?",
            String.class,
            chave);

        assertEquals(esperado, armazenado);
        assertFalse(armazenado.contains(conta.getLogin()));
    }

    @Test
    void naoDeveRegistrarSegredosNosEventos() throws Exception {
        ContaAcesso conta = criarConta("protecao.segredos");
        String chave = registrarChave(conta.getLogin());

        mockMvc.perform(login(conta.getLogin(), "senha-errada"))
            .andExpect(status().isUnauthorized());

        List<Map<String, Object>> eventos = jdbcTemplate.queryForList(
            "SELECT * FROM evento_seguranca WHERE identificador_hash = ?",
            chave);

        assertFalse(eventos.isEmpty());

        for (Map<String, Object> evento : eventos) {
            for (Object valor : evento.values()) {
                if (valor == null) {
                    continue;
                }

                String texto = valor.toString();

                assertFalse(texto.contains("senha-errada"));
                assertFalse(texto.contains("{bcrypt}"));
                assertFalse(texto.contains(conta.getLogin()));
            }
        }
    }

    // ---------------------------------------------------------------
    // Apoio
    // ---------------------------------------------------------------

    private String registrarChave(String login) {
        String chave = sha256Hex(login.trim().toLowerCase(Locale.ROOT));
        chavesCriadas.add(chave);
        return chave;
    }

    private int contarSessoes(Long contaId) {
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sessao_refresh_token "
                + "WHERE conta_acesso_id = ?",
            Integer.class,
            contaId);

        return total == null ? 0 : total;
    }

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

    private ContaAcesso criarConta(String login) {
        long semente = Math.abs(System.nanoTime());

        Pessoa pessoa = pessoaRepository.saveAndFlush(new Pessoa(
            "Pessoa Protecao API",
            "protecao." + semente + "@example.com",
            gerarCpf(semente),
            LocalDate.of(2026, 1, 1)));

        ContaAcesso conta = contaRepository.saveAndFlush(
            new ContaAcesso(
                pessoa,
                login,
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
