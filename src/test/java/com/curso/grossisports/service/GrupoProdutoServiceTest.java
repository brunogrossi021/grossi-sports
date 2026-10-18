
package com.curso.grossisports.service;

import com.curso.grossisports.domain.GrupoProduto;
import com.curso.grossisports.domain.Produto;
import com.curso.grossisports.exception.GrupoProdutoComProdutosException;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.GrupoProdutoRepository;
import com.curso.grossisports.repository.ProdutoRepository;
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
class GrupoProdutoServiceTest {

    @Autowired
    private GrupoProdutoService service;

    @Autowired
    private GrupoProdutoRepository repository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Test
    void deveCadastrarGrupo() {
        GrupoProduto grupo = service.cadastrar("Bolas");

        assertEquals("Bolas", grupo.getNome());
    }

    @Test
    void deveRejeitarNomeDuplicado() {
        service.cadastrar("Bolas");

        assertThrows(
            RecursoDuplicadoException.class,
            () -> service.cadastrar("bolas"));
    }

    @Test
    void deveEditarNomeDoGrupo() {
        GrupoProduto grupo = service.cadastrar("Bolas");

        GrupoProduto editado = service.editar(
            grupo.getId(),
            "Bolas de Futebol");

        assertEquals("Bolas de Futebol", editado.getNome());
        assertEquals(grupo.getId(), editado.getId());
    }

    @Test
    void deveRejeitarEdicaoComNomeDuplicado() {
        service.cadastrar("Bolas");
        GrupoProduto outroGrupo = service.cadastrar("Chuteiras");

        assertThrows(
            RecursoDuplicadoException.class,
            () -> service.editar(
                outroGrupo.getId(),
                "Bolas"));
    }

    @Test
    void deveRejeitarEdicaoDeGrupoInexistente() {
        assertThrows(
            RecursoNaoEncontradoException.class,
            () -> service.editar(999999L, "Bolas"));
    }

    @Test
    void deveExcluirGrupoSemProdutos() {
        GrupoProduto grupo = service.cadastrar("Grupo para excluir");

        service.excluir(grupo.getId());

        assertThrows(
            RecursoNaoEncontradoException.class,
            () -> service.buscarPorId(grupo.getId()));
    }

    @Test
    void deveImpedirExclusaoDeGrupoComProdutos() {
        GrupoProduto grupo = service.cadastrar("Grupo com produto");

        Produto produto = new Produto(
            "TESTE-EXCLUSAO-001",
            "Produto de teste",
            BigDecimal.ZERO,
            BigDecimal.TEN,
            LocalDate.now());

        grupo.adicionarProduto(produto);
        produtoRepository.save(produto);

        assertThrows(
            GrupoProdutoComProdutosException.class,
            () -> service.excluir(grupo.getId()));
    }

    @Test
    void deveRejeitarExclusaoDeGrupoInexistente() {
        assertThrows(
            RecursoNaoEncontradoException.class,
            () -> service.excluir(999999L));
    }
}
