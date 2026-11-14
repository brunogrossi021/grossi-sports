package com.curso.grossisports;

import com.curso.grossisports.domain.Colaborador;
import com.curso.grossisports.domain.FuncaoColaborador;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.repository.ColaboradorRepository;
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
class ColaboradorApiTest {

    private static final LocalDate ADMISSAO =
        LocalDate.of(2026, 10, 29);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ColaboradorRepository colaboradorRepository;

    @Test
    void deveCadastrarColaboradorERetornar201() throws Exception {
        Pessoa pessoa = pessoaRepository.saveAndFlush(novaPessoa());
        String matricula = novaMatricula();

        mockMvc.perform(post("/api/colaboradores")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pessoaId": %d,
                      "matricula": "%s",
                      "funcao": "VENDEDOR",
                      "dataAdmissao": "2026-10-29"
                    }
                    """.formatted(pessoa.getId(), matricula)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.matricula").value(matricula))
            .andExpect(jsonPath("$.funcao").value("VENDEDOR"));
    }

    @Test
    void deveRecusarMatriculaDuplicada() throws Exception {
        String matricula = novaMatricula();
        colaboradorRepository.saveAndFlush(new Colaborador(
            pessoaRepository.saveAndFlush(novaPessoa()),
            matricula, FuncaoColaborador.VENDEDOR, ADMISSAO));

        Pessoa outra = pessoaRepository.saveAndFlush(novaPessoa());

        mockMvc.perform(post("/api/colaboradores")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pessoaId": %d,
                      "matricula": "%s",
                      "funcao": "ESTOQUISTA",
                      "dataAdmissao": "2026-10-29"
                    }
                    """.formatted(outra.getId(), matricula)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                .value("Matrícula já cadastrada"));
    }

    @Test
    void deveBuscarColaboradorPorId() throws Exception {
        Colaborador colaborador = colaboradorRepository.saveAndFlush(
            new Colaborador(
                pessoaRepository.saveAndFlush(novaPessoa()),
                novaMatricula(), FuncaoColaborador.GERENTE,
                ADMISSAO));

        mockMvc.perform(
                get("/api/colaboradores/" + colaborador.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.funcao").value("GERENTE"));
    }

    @Test
    void deveRetornar404AoBuscarColaboradorInexistente()
        throws Exception {

        mockMvc.perform(get("/api/colaboradores/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message")
                .value("Colaborador não encontrado"));
    }

    @Test
    void devePesquisarColaboradoresPaginado() throws Exception {
        mockMvc.perform(get("/api/colaboradores")
                .param("pagina", "0")
                .param("tamanho", "10")
                .param("ordenarPor", "id")
                .param("direcao", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void deveAlterarColaborador() throws Exception {
        Colaborador colaborador = colaboradorRepository.saveAndFlush(
            new Colaborador(
                pessoaRepository.saveAndFlush(novaPessoa()),
                novaMatricula(), FuncaoColaborador.VENDEDOR,
                ADMISSAO));

        mockMvc.perform(put("/api/colaboradores/" + colaborador.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "matricula": "%s",
                      "funcao": "ADMINISTRATIVO"
                    }
                    """.formatted(novaMatricula())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.funcao")
                .value("ADMINISTRATIVO"));
    }

    private String novaMatricula() {
        return "MAT-" + Math.floorMod(
            System.nanoTime(), 1_000_000L);
    }

    private Pessoa novaPessoa() {
        return new Pessoa(
            "Colaborador API",
            "colaborador." + System.nanoTime() + "@example.com",
            gerarCpf(),
            ADMISSAO);
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
