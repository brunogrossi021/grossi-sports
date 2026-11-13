package com.curso.grossisports.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "venda")
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVenda status;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @OneToMany(
        mappedBy = "venda",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<ItemVenda> itens = new ArrayList<>();

    protected Venda() {
    }

    public Venda(Cliente cliente, Colaborador colaborador) {
        this(cliente, colaborador, LocalDateTime.now());
    }

    public Venda(
        Cliente cliente,
        Colaborador colaborador,
        LocalDateTime dataAbertura
    ) {
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente é obrigatório");
        }

        if (colaborador == null) {
            throw new IllegalArgumentException("Colaborador é obrigatório");
        }

        if (dataAbertura == null) {
            throw new IllegalArgumentException(
                "Data de abertura é obrigatória"
            );
        }

        if (colaborador.getFuncao() != FuncaoColaborador.VENDEDOR
            && colaborador.getFuncao() != FuncaoColaborador.GERENTE) {
            throw new IllegalArgumentException(
                "Colaborador não pode realizar vendas"
            );
        }

        this.cliente = cliente;
        this.colaborador = colaborador;
        this.status = StatusVenda.ABERTA;
        this.dataAbertura = dataAbertura;
    }

    public void adicionarItem(
        Produto produto,
        BigDecimal quantidade
    ) {
        validarVendaAberta();

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

        boolean produtoJaAdicionado = itens.stream()
            .anyMatch(item ->
                item.getProduto().getId() != null
                    && item.getProduto().getId().equals(produto.getId())
            );

        if (produtoJaAdicionado) {
            throw new IllegalArgumentException(
                "Produto já foi adicionado à venda"
            );
        }

        ItemVenda item = new ItemVenda(
            this,
            produto,
            quantidade,
            produto.getValorUnitario()
        );

        itens.add(item);
    }

    public void removerItem(Long itemId) {
        validarVendaAberta();

        boolean removido = itens.removeIf(
            item -> item.getId() != null
                && item.getId().equals(itemId)
        );

        if (!removido) {
            throw new IllegalArgumentException(
                "Item não encontrado na venda"
            );
        }
    }

    public BigDecimal calcularTotal() {
        return itens.stream()
            .map(ItemVenda::calcularSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    public void finalizar() {
        validarVendaAberta();

        if (itens.isEmpty()) {
            throw new IllegalStateException(
                "Não é possível finalizar uma venda sem itens"
            );
        }

        this.status = StatusVenda.FINALIZADA;
    }

    public void cancelar() {
        validarVendaAberta();
        this.status = StatusVenda.CANCELADA;
    }

    private void validarVendaAberta() {
        if (status != StatusVenda.ABERTA) {
            throw new IllegalStateException(
                "A operação só é permitida em vendas abertas"
            );
        }
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Colaborador getColaborador() {
        return colaborador;
    }

    public StatusVenda getStatus() {
        return status;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public List<ItemVenda> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
