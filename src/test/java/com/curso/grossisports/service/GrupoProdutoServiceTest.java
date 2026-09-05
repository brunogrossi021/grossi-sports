package com.curso.grossisports.service;

import com.curso.grossisports.domain.GrupoProduto;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.repository.GrupoProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
}
