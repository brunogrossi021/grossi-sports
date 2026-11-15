package com.curso.grossisports;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.PerfilAcesso;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.repository.ContaAcessoRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Consulta administrativa da trilha de auditoria (Aula 17).
 *
 * <p>A classe é {@code @Transactional} apenas para isolar os dados de
 * cada teste: a leitura acontece na mesma transação das ações, o que é
 * suficiente para observar os eventos gravados. A autorização em duas
 * camadas é comprovada pelo 403 do OPERADOR e pelo 200 do
 * ADMINISTRADOR.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditoriaSegurancaApiTest {

    private static final String SENHA = "uma-senha-didatica-2026";
    private static final String NOVA_SENHA =
        "outra-senha-didatica-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------
    // Autorização
    // ---------------------------------------------------------------

    @Test
    void operadorRecebe403NaConsultaDeAuditoria() throws Exception {
        salvarConta("operador.auditoria", SENHA);
        String token = autenticar("operador.auditoria", SENHA);

        mockMvc.perform(get("/api/auditoria-seguranca")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void administradorConsultaComPaginacao() throws Exception {
        String token = tokenAdministrador();

        mockMvc.perform(get("/api/auditoria-seguranca")
                .param("page", "0")
                .param("size", "5")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements",
                greaterThanOrEqualTo(1)));
    }

    @Test
    void dtoNaoExpoeIdentificadorHash() throws Exception {
        String token = tokenAdministrador();

        mockMvc.perform(get("/api/auditoria-seguranca")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].tipo", isA(String.class)))
            .andExpect(jsonPath("$.content[0].identificadorHash")
                .doesNotExist())
            .andExpect(jsonPath("$.content[0].senha").doesNotExist())
            .andExpect(jsonPath("$.content[0].senhaHash").doesNotExist())
            .andExpect(jsonPath("$.content[0].accessToken")
                .doesNotExist())
            .andExpect(jsonPath("$.content[0].refreshToken")
                .doesNotExist());
    }

    @Test
    void deveOrdenarCronologicamenteDoMaisRecenteParaOMaisAntigo()
        throws Exception {

        String token = tokenAdministrador();

        mockMvc.perform(post("/api/contas-acesso")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoNovaConta("ordenacao.auditoria")))
            .andExpect(status().isCreated());

        String corpo = mockMvc.perform(get("/api/auditoria-seguranca")
                .param("size", "100")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].tipo",
                hasItem("CONTA_CRIADA")))
            .andReturn().getResponse().getContentAsString();

        List<String> instantes =
            JsonPath.read(corpo, "$.content[*].ocorridoEm");

        assertFalse(instantes.isEmpty());

        // A trilha é lida do mais recente para o mais antigo: nenhum
        // instante pode ser posterior ao seu antecessor. A asserção é
        // sobre a ordenação em si, não sobre qual evento é o primeiro —
        // o banco de testes compartilha a tabela com outras classes.
        for (int i = 1; i < instantes.size(); i++) {
            assertTrue(
                !Instant.parse(instantes.get(i))
                    .isAfter(Instant.parse(instantes.get(i - 1))),
                "A trilha deve estar em ordem cronológica decrescente");
        }
    }

    // ---------------------------------------------------------------
    // Filtro e validação de parâmetros
    // ---------------------------------------------------------------

    @Test
    void deveFiltrarPorTipoDeEvento() throws Exception {
        String token = tokenAdministrador();

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/contas-acesso")
                    .header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoNovaConta("filtro." + i)))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/auditoria-seguranca")
                .param("tipo", "CONTA_CRIADA")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements",
                greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.content[*].tipo",
                hasItem("CONTA_CRIADA")));
    }

    @Test
    void deveRejeitarTipoDeEventoInvalido() throws Exception {
        String token = tokenAdministrador();

        mockMvc.perform(get("/api/auditoria-seguranca")
                .param("tipo", "NAO_EXISTE")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarTamanhoDePaginaInvalido() throws Exception {
        String token = tokenAdministrador();

        mockMvc.perform(get("/api/auditoria-seguranca")
                .param("size", "0")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/auditoria-seguranca")
                .param("size", "500")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // Ações sensíveis geram eventos
    // ---------------------------------------------------------------

    @Test
    void deveAuditarCriacaoEAlteracoesAdministrativas()
        throws Exception {

        ContaAcesso alvo = salvarConta("alvo.auditoria", SENHA);
        String token = tokenAdministrador();

        mockMvc.perform(post("/api/contas-acesso")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoNovaConta("novo.auditoria")))
            .andExpect(status().isCreated());

        mockMvc.perform(put("/api/contas-acesso/{id}/perfis",
                alvo.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"perfis\": [\"OPERADOR\", \"ADMINISTRADOR\"]}"))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/contas-acesso/{id}/status",
                alvo.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"BLOQUEADA\"}"))
            .andExpect(status().isOk());

        for (String tipo : new String[]{
            "CONTA_CRIADA", "PERFIS_ALTERADOS", "STATUS_ALTERADO",
            "LOGIN_SUCESSO"}) {

            mockMvc.perform(get("/api/auditoria-seguranca")
                    .param("tipo", tipo)
                    .header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements",
                    greaterThanOrEqualTo(1)));
        }
    }

    @Test
    void deveAuditarSenhaRefreshELogout() throws Exception {
        salvarConta("operador.sessao.auditoria", SENHA);

        String corpo = mockMvc
            .perform(login("operador.sessao.auditoria", SENHA))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(corpo, "$.accessToken");
        String refreshToken = JsonPath.read(corpo, "$.refreshToken");

        mockMvc.perform(put("/api/contas-acesso/me/senha")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "senhaAtual": "%s",
                      "novaSenha": "%s"
                    }
                    """.formatted(SENHA, NOVA_SENHA)))
            .andExpect(status().isNoContent());

        String renovado = mockMvc.perform(post("/api/autenticacao/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"%s\"}"
                    .formatted(refreshToken)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String refreshNovo = JsonPath.read(renovado, "$.refreshToken");

        mockMvc.perform(post("/api/autenticacao/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"%s\"}"
                    .formatted(refreshNovo)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/autenticacao/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"token-inexistente\"}"))
            .andExpect(status().isUnauthorized());

        String adminToken = tokenAdministrador();

        for (String tipo : new String[]{
            "SENHA_ALTERADA", "REFRESH_SUCESSO", "REFRESH_FALHA",
            "LOGOUT"}) {

            mockMvc.perform(get("/api/auditoria-seguranca")
                    .param("tipo", tipo)
                    .header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements",
                    greaterThanOrEqualTo(1)));
        }
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

    private String autenticar(String login, String senha)
        throws Exception {

        String corpo = mockMvc.perform(login(login, senha))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        return JsonPath.read(corpo, "$.accessToken");
    }

    private String tokenAdministrador() throws Exception {
        salvarContaAdmin("admin.auditoria", SENHA);
        return autenticar("admin.auditoria", SENHA);
    }

    private String corpoNovaConta(String login) {
        long semente = Math.abs(System.nanoTime());

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
