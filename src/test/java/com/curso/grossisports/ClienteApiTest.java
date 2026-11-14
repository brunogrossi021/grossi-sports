package com.curso.grossisports;

import com.curso.grossisports.domain.Cliente;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.repository.ClienteRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class ClienteApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void deveCadastrarClienteERetornar201() throws Exception {
        Pessoa pessoa = pessoaRepository.saveAndFlush(novaPessoa());

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pessoaId": %d,
                      "telefone": "11 99999-0000"
                    }
                    """.formatted(pessoa.getId())))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.pessoaId")
                .value(pessoa.getId().intValue()))
            .andExpect(jsonPath("$.telefone")
                .value("11 99999-0000"));
    }

    @Test
    void deveRetornar404ParaPessoaInexistente() throws Exception {
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pessoaId": 999999,
                      "telefone": "11 99999-0000"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message")
                .value("Pessoa não encontrada"));
    }

    @Test
    void deveRecusarClienteDePessoaInativa() throws Exception {
        Pessoa pessoa = novaPessoa();
        pessoa.inativar();
        pessoaRepository.saveAndFlush(pessoa);

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pessoaId": %d,
                      "telefone": "11 99999-0000"
                    }
                    """.formatted(pessoa.getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Pessoa deve estar ativa"));
    }

    @Test
    void deveBuscarClientePorId() throws Exception {
        Cliente cliente = clienteRepository.saveAndFlush(
            new Cliente(
                pessoaRepository.saveAndFlush(novaPessoa()),
                "11 99999-0000"));

        mockMvc.perform(get("/api/clientes/" + cliente.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.telefone")
                .value("11 99999-0000"));
    }

    @Test
    void deveRetornar404AoBuscarClienteInexistente() throws Exception {
        mockMvc.perform(get("/api/clientes/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message")
                .value("Cliente não encontrado"));
    }

    @Test
    void devePesquisarClientesPaginado() throws Exception {
        mockMvc.perform(get("/api/clientes")
                .param("pagina", "0")
                .param("tamanho", "10")
                .param("ordenarPor", "id")
                .param("direcao", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void deveAlterarTelefoneDoCliente() throws Exception {
        Cliente cliente = clienteRepository.saveAndFlush(
            new Cliente(
                pessoaRepository.saveAndFlush(novaPessoa()),
                "11 99999-0000"));

        mockMvc.perform(put("/api/clientes/" + cliente.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefone\": \"11 98888-1111\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.telefone")
                .value("11 98888-1111"));
    }

    private Pessoa novaPessoa() {
        return new Pessoa(
            "Cliente API",
            "cliente." + System.nanoTime() + "@example.com",
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
