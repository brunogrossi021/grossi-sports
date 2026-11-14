package com.curso.grossisports;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.GrupoProduto;
import com.curso.grossisports.domain.PerfilAcesso;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.repository.ContaAcessoRepository;
import com.curso.grossisports.repository.GrupoProdutoRepository;
import com.curso.grossisports.repository.PessoaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorização por perfis (Aula 14): verifica 401, 403, 200, 201 e 400
 * para OPERADOR e ADMINISTRADOR, além das invariantes administrativas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AutorizacaoApiTest {

    private static final String SENHA = "uma-senha-didatica-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Autowired
    private GrupoProdutoRepository grupoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------
    // Criação de conta
    // ---------------------------------------------------------------

    @Test
    void deveNegarCriacaoDeContaSemToken() throws Exception {
        mockMvc.perform(post("/api/contas-acesso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoNovaConta("sem.token")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveNegarCriacaoDeContaParaOperador() throws Exception {
        salvarConta("operador.criacao", SENHA);
        String token = autenticar("operador.criacao", SENHA);

        mockMvc.perform(post("/api/contas-acesso")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoNovaConta("operador.criacao")))
            .andExpect(status().isForbidden());
    }

    @Test
    void devePermitirCriacaoDeContaParaAdministrador()
        throws Exception {

        String token = tokenAdministrador();

        mockMvc.perform(post("/api/contas-acesso")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoNovaConta("novo.operador")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("novo.operador"))
            .andExpect(jsonPath("$.status").value("ATIVA"))
            .andExpect(jsonPath("$.perfis[0]").value("OPERADOR"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    // ---------------------------------------------------------------
    // Alteração de perfis e status
    // ---------------------------------------------------------------

    @Test
    void devePermitirAdministradorAlterarPerfis() throws Exception {
        ContaAcesso alvo = salvarConta("alvo.perfis", SENHA);
        String token = tokenAdministrador();

        mockMvc.perform(put("/api/contas-acesso/{id}/perfis", alvo.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"perfis": ["OPERADOR", "ADMINISTRADOR"]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.perfis",
                hasItems("OPERADOR", "ADMINISTRADOR")));
    }

    @Test
    void deveNegarAlteracaoDePerfisParaOperador() throws Exception {
        ContaAcesso alvo = salvarConta("alvo.perfis.op", SENHA);
        salvarConta("operador.perfis", SENHA);
        String token = autenticar("operador.perfis", SENHA);

        mockMvc.perform(put("/api/contas-acesso/{id}/perfis", alvo.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"perfis": ["ADMINISTRADOR"]}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void deveRejeitarListaDePerfisVazia() throws Exception {
        ContaAcesso alvo = salvarConta("alvo.vazio", SENHA);
        String token = tokenAdministrador();

        mockMvc.perform(put("/api/contas-acesso/{id}/perfis", alvo.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"perfis": []}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void devePermitirAdministradorBloquearOutraConta() throws Exception {
        ContaAcesso alvo = salvarConta("alvo.bloqueio", SENHA);
        String token = tokenAdministrador();

        mockMvc.perform(put("/api/contas-acesso/{id}/status", alvo.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status": "BLOQUEADA"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("BLOQUEADA"));
    }

    @Test
    void deveRejeitarStatusInvalido() throws Exception {
        ContaAcesso alvo = salvarConta("alvo.status", SENHA);
        String token = tokenAdministrador();

        mockMvc.perform(put("/api/contas-acesso/{id}/status", alvo.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status": "INEXISTENTE"}
                    """))
            .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // Invariantes administrativas
    // ---------------------------------------------------------------

    @Test
    void administradorNaoPodeBloquearASiMesmo() throws Exception {
        ContaAcesso admin = salvarContaAdmin("admin.auto", SENHA);
        String token = autenticar("admin.auto", SENHA);

        mockMvc.perform(put("/api/contas-acesso/{id}/status", admin.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status": "BLOQUEADA"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void administradorNaoPodeRemoverProprioPerfilAdministrador()
        throws Exception {

        ContaAcesso admin = salvarContaAdmin("admin.auto.rebaixa", SENHA);
        String token = autenticar("admin.auto.rebaixa", SENHA);

        mockMvc.perform(put("/api/contas-acesso/{id}/perfis", admin.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"perfis": ["OPERADOR"]}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void naoDeveBloquearUltimoAdministradorAtivo() throws Exception {
        ContaAcesso adminA = salvarContaAdmin("admin.a", SENHA);
        ContaAcesso adminB = salvarContaAdmin("admin.b", SENHA);

        String tokenA = autenticar("admin.a", SENHA);
        String tokenB = autenticar("admin.b", SENHA);

        // A bloqueia B: ainda resta um administrador ativo.
        mockMvc.perform(put("/api/contas-acesso/{id}/status", adminB.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status": "BLOQUEADA"}
                    """))
            .andExpect(status().isOk());

        // O token de B continua válido (JWT autocontido): tentar
        // bloquear o último administrador ativo deve falhar.
        mockMvc.perform(put("/api/contas-acesso/{id}/status", adminA.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status": "BLOQUEADA"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void naoDeveRemoverPerfilDoUltimoAdministradorAtivo()
        throws Exception {

        ContaAcesso adminA = salvarContaAdmin("admin.c", SENHA);
        ContaAcesso adminB = salvarContaAdmin("admin.d", SENHA);

        String tokenA = autenticar("admin.c", SENHA);
        String tokenB = autenticar("admin.d", SENHA);

        mockMvc.perform(put("/api/contas-acesso/{id}/status", adminB.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status": "BLOQUEADA"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/contas-acesso/{id}/perfis", adminA.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"perfis": ["OPERADOR"]}
                    """))
            .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // Operações cotidianas e exclusão administrativa
    // ---------------------------------------------------------------

    @Test
    void operadorPodeExecutarOperacaoCotidiana() throws Exception {
        salvarConta("operador.cotidiano", SENHA);
        String token = autenticar("operador.cotidiano", SENHA);

        mockMvc.perform(get("/api/produtos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void operadorNaoPodeExcluirGrupo() throws Exception {
        salvarConta("operador.exclusao", SENHA);
        String token = autenticar("operador.exclusao", SENHA);

        mockMvc.perform(delete("/api/grupos-produtos/{id}", 999999L)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void administradorPodeExcluirGrupo() throws Exception {
        GrupoProduto grupo = grupoRepository.save(
            new GrupoProduto("Grupo Exclusão Admin"));
        String token = tokenAdministrador();

        mockMvc.perform(delete("/api/grupos-produtos/{id}", grupo.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------
    // Apoio
    // ---------------------------------------------------------------

    private String corpoNovaConta(String login) {
        long semente = System.nanoTime();
        return """
            {
              "nome": "Pessoa %s",
              "email": "%s.%d@example.com",
              "cpf": "%s",
              "login": "%s",
              "senha": "%s"
            }
            """.formatted(
            login,
            login,
            semente,
            gerarCpf(semente),
            login,
            SENHA);
    }

    private MockHttpServletRequestBuilder loginRequest(
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

    private String autenticar(String login, String senha)
        throws Exception {

        String corpo = mockMvc.perform(loginRequest(login, senha))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return JsonPath.read(corpo, "$.accessToken");
    }

    private String tokenAdministrador() throws Exception {
        salvarContaAdmin("admin.autorizacao", SENHA);
        return autenticar("admin.autorizacao", SENHA);
    }

    private ContaAcesso salvarConta(String login, String senha) {
        return contaRepository.saveAndFlush(new ContaAcesso(
            novaPessoa(login),
            login,
            passwordEncoder.encode(senha)));
    }

    private ContaAcesso salvarContaAdmin(String login, String senha) {
        ContaAcesso conta = salvarConta(login, senha);
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        return contaRepository.saveAndFlush(conta);
    }

    private Pessoa novaPessoa(String identificador) {
        long semente = Math.abs(System.nanoTime());
        return pessoaRepository.saveAndFlush(new Pessoa(
            "Pessoa " + identificador,
            identificador + "." + semente + "@example.com",
            gerarCpf(semente),
            LocalDate.of(2026, 10, 29)));
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
