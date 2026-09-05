package com.curso.grossisports.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
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
class PersistenciaJpaTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void devePersistirERelerGrupoEProduto() {
        GrupoProduto grupo = new GrupoProduto("Periféricos");

        Produto produto = new Produto(
            "7891000000019",
            "Mouse sem fio",
            new BigDecimal("10.000"),
            new BigDecimal("89.90"),
            LocalDate.of(2026, 3, 10));

        grupo.adicionarProduto(produto);

        entityManager.persist(grupo);
        entityManager.persist(produto);
        entityManager.flush();

        Long produtoId = produto.getId();

        entityManager.clear();

        Produto recuperado =
            entityManager.find(Produto.class, produtoId);

        assertEquals(
            "Periféricos",
            recuperado.getGrupo().getNome());
    }

    @Test
    void deveTerOitoChangeSetsRegistrados() {
        Long quantidade = ((Number) entityManager
            .createNativeQuery(
                "SELECT COUNT(*) FROM databasechangelog")
            .getSingleResult())
            .longValue();

        assertEquals(8L, quantidade);
    }

    @Test
    @Transactional
    void deveRejeitarCodigoDeBarrasDuplicadoNoBanco() {
        GrupoProduto grupo1 = new GrupoProduto("Periféricos");
        GrupoProduto grupo2 = new GrupoProduto("Acessórios");

        Produto produto1 = new Produto(
            "7891000000026",
            "Teclado",
            new BigDecimal("5.000"),
            new BigDecimal("120.00"),
            LocalDate.of(2026, 3, 10));

        Produto produto2 = new Produto(
            "7891000000026",
            "Headset",
            new BigDecimal("3.000"),
            new BigDecimal("150.00"),
            LocalDate.of(2026, 3, 10));

        grupo1.adicionarProduto(produto1);
        grupo2.adicionarProduto(produto2);

        entityManager.persist(grupo1);
        entityManager.persist(produto1);
        entityManager.persist(grupo2);

        assertThrows(
            PersistenceException.class,
            () -> entityManager.persist(produto2));
    }

    @Test
    @Transactional
    void deveRejeitarSaldoDeEstoqueNegativoNoBanco() {
        GrupoProduto grupo = new GrupoProduto("Bolas");

        Produto produto = new Produto(
            "7891000000033",
            "Bola de futebol",
            new BigDecimal("1.000"),
            new BigDecimal("99.90"),
            LocalDate.of(2026, 3, 10));

        grupo.adicionarProduto(produto);

        entityManager.persist(grupo);
        entityManager.persist(produto);
        entityManager.flush();

        assertThrows(
            PersistenceException.class,
            () -> entityManager.createNativeQuery(
                    "UPDATE produto SET saldo_estoque = -1.000 WHERE id = :id")
                .setParameter("id", produto.getId())
                .executeUpdate());
    }
}
