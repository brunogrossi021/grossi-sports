package com.curso.grossisports;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.PerfilAcesso;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.repository.ContaAcessoRepository;
import com.curso.grossisports.repository.PessoaRepository;
import com.curso.grossisports.security.JwtProperties;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContaAcessoSecurityTest {

    private static final String SENHA = "uma-senha-didatica-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void devePermitirHealthSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk());
    }

    @Test
    void deveNegarRotaProtegidaSemToken() throws Exception {
        mockMvc.perform(get("/api/produtos"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveCadastrarContaArmazenandoHashSemExporSenha()
        throws Exception {

        String token = tokenAdministrador();
        String cpf = gerarCpf(System.nanoTime());

        mockMvc.perform(post("/api/contas-acesso")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nome": "Pessoa Cadastro",
                      "email": "cadastro.%d@example.com",
                      "cpf": "%s",
                      "login": "ALUNA.2026",
                      "senha": "%s"
                    }
                    """.formatted(System.nanoTime(), cpf, SENHA)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("aluna.2026"))
            .andExpect(jsonPath("$.status").value("ATIVA"))
            .andExpect(jsonPath("$.perfis[0]").value("OPERADOR"))
            .andExpect(jsonPath("$.senha").doesNotExist())
            .andExpect(jsonPath("$.senhaHash").doesNotExist());

        ContaAcesso conta = contaRepository
            .findByLoginIgnoreCase("aluna.2026")
            .orElseThrow();

        assertNotEquals(SENHA, conta.getSenhaHash());
        assertTrue(conta.getSenhaHash().startsWith("{bcrypt}"));
        assertTrue(passwordEncoder.matches(SENHA, conta.getSenhaHash()));
    }

    @Test
    void deveEmitirTokenBearerNoLogin() throws Exception {
        salvarConta("operadora.2026", SENHA);

        mockMvc.perform(loginRequest("operadora.2026", SENHA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(1800))
            .andExpect(jsonPath("$.expiresAt").exists())
            .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void deveEmitirClaimsEsperadosNoToken() throws Exception {
        salvarConta("operadora.2026", SENHA);

        Jwt jwt = jwtDecoder.decode(autenticar("operadora.2026", SENHA));

        assertEquals(
            jwtProperties.issuer(), jwt.getIssuer().toString());
        assertEquals("operadora.2026", jwt.getSubject());
        assertNotNull(jwt.getId());
        assertFalse(jwt.getId().isBlank());
        assertTrue(jwt.getClaimAsStringList("perfis")
            .contains("OPERADOR"));
        assertTrue(jwt.getExpiresAt().isAfter(jwt.getIssuedAt()));
    }

    @Test
    void deveAcessarRotaProtegidaComTokenValido() throws Exception {
        salvarConta("operadora.2026", SENHA);

        String token = autenticar("operadora.2026", SENHA);

        mockMvc.perform(get("/api/contas-acesso/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("operadora.2026"))
            .andExpect(jsonPath("$.nomePessoa",
                containsString("Pessoa")));
    }

    @Test
    void deveNegarBasicMesmoComCredenciaisValidas() throws Exception {
        salvarConta("operadora.2026", SENHA);

        mockMvc.perform(get("/api/contas-acesso/me")
                .with(httpBasic("operadora.2026", SENHA)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveNegarLoginComSenhaIncorreta() throws Exception {
        salvarConta("operadora.2026", SENHA);

        mockMvc.perform(loginRequest("operadora.2026", "senha-incorreta"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(
                "Login ou senha inválidos"));
    }

    @Test
    void deveNegarLoginDeContaBloqueada() throws Exception {
        ContaAcesso conta = salvarConta("conta.bloqueada", SENHA);
        conta.bloquear();
        contaRepository.saveAndFlush(conta);

        mockMvc.perform(loginRequest("conta.bloqueada", SENHA))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(
                "Login ou senha inválidos"));
    }

    @Test
    void deveNegarTokenMalformado() throws Exception {
        mockMvc.perform(get("/api/contas-acesso/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer nao-e-um-jwt"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveNegarTokenComAssinaturaAlterada() throws Exception {
        salvarConta("operadora.2026", SENHA);

        String token = autenticar("operadora.2026", SENHA);
        String[] partes = token.split("\\.");

        char primeiro = partes[2].charAt(0);
        char trocado = primeiro == 'A' ? 'B' : 'A';

        String adulterado = partes[0] + "." + partes[1] + "."
            + trocado + partes[2].substring(1);

        mockMvc.perform(get("/api/contas-acesso/me")
                .header(HttpHeaders.AUTHORIZATION,
                    "Bearer " + adulterado))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveNegarTokenExpirado() throws Exception {
        salvarConta("operadora.2026", SENHA);

        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .issuedAt(agora.minusSeconds(3600))
            .expiresAt(agora.minusSeconds(60))
            .subject("operadora.2026")
            .id(UUID.randomUUID().toString())
            .claim("perfis", List.of("OPERADOR"))
            .build();

        String expirado = jwtEncoder
            .encode(JwtEncoderParameters.from(claims))
            .getTokenValue();

        mockMvc.perform(get("/api/contas-acesso/me")
                .header(HttpHeaders.AUTHORIZATION,
                    "Bearer " + expirado))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveVincularContaAPessoaCriadaNaPropriaRequisicao()
        throws Exception {

        long semente = System.nanoTime();
        String email = "vinculo." + semente + "@example.com";
        String cpf = gerarCpf(semente);
        String login = "vinculo." + semente;
        String token = tokenAdministrador();

        mockMvc.perform(post("/api/contas-acesso")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nome": "Pessoa Vínculo",
                      "email": "%s",
                      "cpf": "%s",
                      "login": "%s",
                      "senha": "%s"
                    }
                    """.formatted(email, cpf, login, SENHA)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.pessoaId").isNumber())
            .andExpect(jsonPath("$.nomePessoa").value("Pessoa Vínculo"));

        ContaAcesso conta = contaRepository
            .findByLoginIgnoreCase(login)
            .orElseThrow();

        Pessoa pessoa = conta.getPessoa();

        assertEquals(
            email.toLowerCase(Locale.ROOT), pessoa.getEmail());
        assertEquals(cpf, pessoa.getCpf());
        assertEquals("Pessoa Vínculo", pessoa.getNome());
        assertTrue(contaRepository.existsByPessoaId(pessoa.getId()));
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

    private ContaAcesso salvarConta(String login, String senha) {
        Pessoa pessoa = novaPessoa(login.replace('.', '-'));
        return contaRepository.saveAndFlush(
            new ContaAcesso(
                pessoa, login, passwordEncoder.encode(senha)));
    }

    private String tokenAdministrador() throws Exception {
        salvarContaAdmin("admin.2026", SENHA);
        return autenticar("admin.2026", SENHA);
    }

    private ContaAcesso salvarContaAdmin(String login, String senha) {
        Pessoa pessoa = novaPessoa(login.replace('.', '-'));
        ContaAcesso conta = new ContaAcesso(
            pessoa, login, passwordEncoder.encode(senha));
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        return contaRepository.saveAndFlush(conta);
    }

    private Pessoa novaPessoa(String identificador) {
        long semente = Math.abs(System.nanoTime());
        Pessoa pessoa = new Pessoa(
            "Pessoa " + identificador,
            identificador + "." + semente + "@example.com",
            gerarCpf(semente),
            LocalDate.of(2026, 10, 29));
        return pessoaRepository.saveAndFlush(pessoa);
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
