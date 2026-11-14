package com.curso.grossisports;

import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.repository.PessoaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class PessoaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Test
    void deveCadastrarPessoaERetornar201() throws Exception {
        String cpf = gerarCpf();

        mockMvc.perform(post("/api/pessoas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nome": "Pessoa API",
                      "email": "pessoa.api@example.com",
                      "cpf": "%s"
                    }
                    """.formatted(cpf)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.nome").value("Pessoa API"))
            .andExpect(jsonPath("$.cpf").value(cpf))
            .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void deveRecusarPessoaComNomeVazio() throws Exception {
        mockMvc.perform(post("/api/pessoas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nome": "",
                      "email": "pessoa.api@example.com",
                      "cpf": "52998224725"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Dados inválidos"))
            .andExpect(jsonPath("$.fields.nome")
                .value("Nome é obrigatório"));
    }

    @Test
    void deveRetornar404AoBuscarPessoaInexistente() throws Exception {
        mockMvc.perform(get("/api/pessoas/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message")
                .value("Pessoa não encontrada"));
    }

    @Test
    void devePesquisarPessoasPaginado() throws Exception {
        pessoaRepository.saveAndFlush(novaPessoa());

        mockMvc.perform(get("/api/pessoas")
                .param("pagina", "0")
                .param("tamanho", "10")
                .param("ordenarPor", "nome")
                .param("direcao", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void deveAlterarPessoa() throws Exception {
        Pessoa pessoa = pessoaRepository.saveAndFlush(novaPessoa());

        mockMvc.perform(put("/api/pessoas/" + pessoa.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nome": "Pessoa Alterada",
                      "email": "%s",
                      "cpf": "%s"
                    }
                    """.formatted(pessoa.getEmail(), pessoa.getCpf())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Pessoa Alterada"));
    }

    @Test
    void deveAlterarStatusEUsarAtalhos() throws Exception {
        Pessoa pessoa = pessoaRepository.saveAndFlush(novaPessoa());

        mockMvc.perform(
                put("/api/pessoas/" + pessoa.getId() + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\": \"INATIVO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INATIVO"));

        mockMvc.perform(
                patch("/api/pessoas/" + pessoa.getId() + "/ativar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ATIVO"));

        mockMvc.perform(
                patch("/api/pessoas/" + pessoa.getId() + "/inativar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INATIVO"));
    }

    private Pessoa novaPessoa() {
        return new Pessoa(
            "Pessoa API",
            "pessoa." + System.nanoTime() + "@example.com",
            gerarCpf(),
            LocalDate.of(2026, 10, 29));
    }

    private String gerarCpf() {
        long semente = Math.abs(System.nanoTime());
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
