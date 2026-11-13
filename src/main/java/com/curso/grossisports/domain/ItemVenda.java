package com.curso.grossisports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name = "item_venda")
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorUnitario;

    protected ItemVenda() {
    }

    public ItemVenda(
        Venda venda,
        Produto produto,
        BigDecimal quantidade,
        BigDecimal valorUnitario
    ) {
        if (venda == null) {
            throw new IllegalArgumentException("Venda é obrigatória");
        }

        if (produto == null) {
            throw new IllegalArgumentException("Produto é obrigatório");
        }

        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException(
                "Quantidade deve ser maior que zero"
            );
        }

        if (quantidade.scale() > 3 || quantidade.precision() > 18) {
            throw new IllegalArgumentException(
                "Quantidade deve possuir até 3 casas decimais"
            );
        }

        if (valorUnitario == null || valorUnitario.signum() < 0) {
            throw new IllegalArgumentException(
                "Valor unitário não pode ser negativo"
            );
        }

        this.venda = venda;
        this.produto = produto;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal calcularSubtotal() {
        return valorUnitario
            .multiply(quantidade)
            .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public Venda getVenda() {
        return venda;
    }

    public Produto getProduto() {
        return produto;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }
}
