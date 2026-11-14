package com.curso.grossisports;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class DocumentoFiscalApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCadastrarPessoaComCpfValido() throws Exception {
        String cpf = gerarCpf(System.nanoTime());

        mockMvc.perform(post("/api/pessoas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Pessoa API","email":"pessoa.%d@example.com","cpf":"%s"}
                    """.formatted(System.nanoTime(), cpf)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cpf").value(cpf))
            .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void deveRejeitarCpfComDigitoVerificadorIncorreto()
        throws Exception {

        mockMvc.perform(post("/api/pessoas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Pessoa inválida","email":"invalida@example.com","cpf":"52998224724"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.cpf").value("CPF inválido"));
    }

    @Test
    void deveRejeitarEmailJaCadastrado() throws Exception {
        String email = "duplicado.%d@example.com"
            .formatted(System.nanoTime());

        mockMvc.perform(post("/api/pessoas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Primeira","email":"%s","cpf":"%s"}
                    """.formatted(email, gerarCpf(System.nanoTime()))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/pessoas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Segunda","email":"%s","cpf":"%s"}
                    """.formatted(email, gerarCpf(System.nanoTime() + 7919))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                .value("E-mail já cadastrado"));
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
