package com.curso.grossisports;

import com.curso.grossisports.domain.Fornecedor;
import com.curso.grossisports.domain.GrupoProduto;
import com.curso.grossisports.repository.FornecedorRepository;
import com.curso.grossisports.repository.GrupoProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProdutoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GrupoProdutoRepository grupoRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Test
    void deveCadastrarProdutoERetornar201() throws Exception {

        GrupoProduto grupo = grupoRepository.save(
            new GrupoProduto("Grupo API"));

        Fornecedor fornecedor = fornecedorRepository.save(
            new Fornecedor(
                "Fornecedor API",
                "22222222000192"));

        String json = """
            {
              "codigoBarras": "API-001",
              "descricao": "Produto criado pela API",
              "saldoEstoque": 10.000,
              "valorUnitario": 49.90,
              "estoqueMinimo": 2.000,
              "grupoId": %d,
              "fornecedorId": %d
            }
            """.formatted(
            grupo.getId(),
            fornecedor.getId());

        mockMvc.perform(
                post("/api/produtos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.codigoBarras")
                .value("API-001"));
    }

    @Test
    void deveRecusarProdutoComDescricaoVazia() throws Exception {

        GrupoProduto grupo = grupoRepository.save(
            new GrupoProduto("Grupo Validação"));

        String json = """
            {
              "codigoBarras": "API-002",
              "descricao": "",
              "saldoEstoque": 10.000,
              "valorUnitario": 49.90,
              "estoqueMinimo": 2.000,
              "grupoId": %d
            }
            """.formatted(grupo.getId());

        mockMvc.perform(
                post("/api/produtos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Dados inválidos"))
            .andExpect(jsonPath("$.fields.descricao")
                .value("Descrição é obrigatória"));
    }

    @Test
    void deveRetornar404AoBuscarProdutoInexistente() throws Exception {

        mockMvc.perform(
                get("/api/produtos/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message")
                .value("Produto não encontrado"));
    }
}
