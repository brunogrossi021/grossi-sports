package com.curso.grossisports.service;

import com.curso.grossisports.domain.GrupoProduto;
import com.curso.grossisports.domain.Produto;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.GrupoProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProdutoServiceTest {

    @Autowired
    private ProdutoService service;

    @Autowired
    private GrupoProdutoRepository grupoRepository;

    @Test
    void deveCadastrarProduto() {
        GrupoProduto grupo = new GrupoProduto("Bolas");
        grupoRepository.save(grupo);

        Produto produto = new Produto(
            "7891000000040",
            "Bola de futebol",
            new BigDecimal("10.000"),
            new BigDecimal("99.90"),
            LocalDate.of(2026, 3, 10));

        Produto cadastrado = service.cadastrar(
            produto,
            grupo.getId());

        assertEquals("7891000000040",
            cadastrado.getCodigoBarras());

        assertEquals(grupo.getId(),
            cadastrado.getGrupo().getId());
    }

    @Test
    void deveRejeitarCodigoDeBarrasDuplicado() {
        GrupoProduto grupo = new GrupoProduto("Bolas");
        grupoRepository.save(grupo);

        Produto primeiro = new Produto(
            "7891000000057",
            "Bola oficial",
            new BigDecimal("5.000"),
            new BigDecimal("199.90"),
            LocalDate.of(2026, 3, 10));

        service.cadastrar(primeiro, grupo.getId());

        Produto segundo = new Produto(
            "7891000000057",
            "Outra bola",
            new BigDecimal("3.000"),
            new BigDecimal("149.90"),
            LocalDate.of(2026, 3, 10));

        assertThrows(
            RecursoDuplicadoException.class,
            () -> service.cadastrar(
                segundo,
                grupo.getId()));
    }

    @Test
    void deveRejeitarGrupoInexistente() {
        Produto produto = new Produto(
            "7891000000064",
            "Chuteira",
            new BigDecimal("2.000"),
            new BigDecimal("299.90"),
            LocalDate.of(2026, 3, 10));

        assertThrows(
            RecursoNaoEncontradoException.class,
            () -> service.cadastrar(produto, 999999L));
    }
}
