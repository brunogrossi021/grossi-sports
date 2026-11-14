package com.curso.grossisports;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.repository.ContaAcessoRepository;
import com.curso.grossisports.repository.PessoaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    void devePermitirHealthSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk());
    }

    @Test
    void deveNegarRotaProtegidaSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/produtos"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveCadastrarContaArmazenandoHashSemExporSenha()
        throws Exception {

        String cpf = gerarCpf(System.nanoTime());

        mockMvc.perform(post("/api/contas-acesso")
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
    void deveAutenticarComHttpBasicEConsultarAPropriaConta()
        throws Exception {

        salvarConta("operadora.2026", SENHA);

        mockMvc.perform(get("/api/contas-acesso/me")
                .with(httpBasic("operadora.2026", SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("operadora.2026"))
            .andExpect(jsonPath("$.nomePessoa",
                containsString("Pessoa")));

        mockMvc.perform(get("/api/contas-acesso/me")
                .with(httpBasic("operadora.2026", "senha-incorreta")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveImpedirAutenticacaoDeContaBloqueada()
        throws Exception {

        ContaAcesso conta = salvarConta("conta.bloqueada", SENHA);
        conta.bloquear();
        contaRepository.saveAndFlush(conta);

        mockMvc.perform(get("/api/contas-acesso/me")
                .with(httpBasic("conta.bloqueada", SENHA)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveVincularContaAPessoaCriadaNaPropriaRequisicao()
        throws Exception {

        long semente = System.nanoTime();
        String email = "vinculo." + semente + "@example.com";
        String cpf = gerarCpf(semente);
        String login = "vinculo." + semente;

        mockMvc.perform(post("/api/contas-acesso")
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

    private ContaAcesso salvarConta(String login, String senha) {
        Pessoa pessoa = novaPessoa(login.replace('.', '-'));
        return contaRepository.saveAndFlush(
            new ContaAcesso(
                pessoa, login, passwordEncoder.encode(senha)));
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
