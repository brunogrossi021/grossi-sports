package com.curso.grossisports.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrupoProdutoTest {

    @Test
    void deveCriarGrupoAtivo() {
        GrupoProduto grupo = new GrupoProduto("Futebol");

        assertEquals("Futebol", grupo.getNome());
        assertEquals(Status.ATIVO, grupo.getStatus());
        assertEquals(0, grupo.getProdutos().size());
    }

    @Test
    void deveAdicionarProdutoAoGrupo() {
        GrupoProduto grupo = new GrupoProduto("Futebol");
        Produto produto = novoProduto("7890000000001");

        grupo.adicionarProduto(produto);

        assertEquals(1, grupo.getProdutos().size());
        assertEquals(grupo, produto.getGrupo());
    }

    @Test
    void naoDeveAdicionarDoisProdutosComMesmoCodigoNoGrupo() {
        GrupoProduto grupo = new GrupoProduto("Futebol");

        Produto primeiro = novoProduto("7890000000001");
        Produto segundo = novoProduto("7890000000001");

        grupo.adicionarProduto(primeiro);

        IllegalArgumentException excecao = assertThrows(
            IllegalArgumentException.class,
            () -> grupo.adicionarProduto(segundo));

        assertEquals(
            "Código de barras já utilizado no grupo",
            excecao.getMessage());
    }

    @Test
    void naoDeveAssociarProdutoAOutroGrupo() {
        GrupoProduto primeiroGrupo = new GrupoProduto("Futebol");
        GrupoProduto segundoGrupo = new GrupoProduto("Basquete");

        Produto produto = novoProduto("7890000000001");

        primeiroGrupo.adicionarProduto(produto);

        assertThrows(
            IllegalStateException.class,
            () -> segundoGrupo.adicionarProduto(produto));
    }

    @Test
    void deveAlterarOStatusDoGrupo() {
        GrupoProduto grupo = new GrupoProduto("Futebol");

        grupo.inativar();
        assertEquals(Status.INATIVO, grupo.getStatus());

        grupo.ativar();
        assertEquals(Status.ATIVO, grupo.getStatus());
    }

    private Produto novoProduto(String codigoBarras) {
        return new Produto(
            codigoBarras,
            "Bola de futebol",
            new BigDecimal("10"),
            new BigDecimal("150.00"),
            LocalDate.of(2026, 8, 20));
    }
}
