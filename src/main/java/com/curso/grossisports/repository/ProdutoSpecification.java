
package com.curso.grossisports.repository;

import com.curso.grossisports.domain.Produto;
import com.curso.grossisports.domain.Status;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProdutoSpecification {

    private ProdutoSpecification() {
    }

    public static Specification<Produto> descricaoContem(
        String descricao) {

        return (root, query, builder) -> {
            if (descricao == null || descricao.isBlank()) {
                return builder.conjunction();
            }

            return builder.like(
                builder.lower(root.get("descricao")),
                "%" + descricao.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Produto> porCodigoBarras(
        String codigoBarras) {

        return (root, query, builder) -> {
            if (codigoBarras == null || codigoBarras.isBlank()) {
                return builder.conjunction();
            }

            return builder.equal(
                root.get("codigoBarras"),
                codigoBarras.trim());
        };
    }

    public static Specification<Produto> porGrupo(Long grupoId) {
        return (root, query, builder) -> {
            if (grupoId == null) {
                return builder.conjunction();
            }

            return builder.equal(
                root.get("grupo").get("id"),
                grupoId);
        };
    }

    public static Specification<Produto> porStatus(Status status) {
        return (root, query, builder) -> {
            if (status == null) {
                return builder.conjunction();
            }

            return builder.equal(root.get("status"), status);
        };
    }

    public static Specification<Produto> valorUnitarioMaiorOuIgual(
        BigDecimal valorMinimo) {

        return (root, query, builder) -> {
            if (valorMinimo == null) {
                return builder.conjunction();
            }

            return builder.greaterThanOrEqualTo(
                root.get("valorUnitario"),
                valorMinimo);
        };
    }

    public static Specification<Produto> valorUnitarioMenorOuIgual(
        BigDecimal valorMaximo) {

        return (root, query, builder) -> {
            if (valorMaximo == null) {
                return builder.conjunction();
            }

            return builder.lessThanOrEqualTo(
                root.get("valorUnitario"),
                valorMaximo);
        };
    }
}
