package com.curso.grossisports.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VendaTest {

    @Test
    void deveCalcularTotalComPrecoCapturadoNoMomentoDaInclusao() {
        Venda venda = novaVenda();
        Produto produto = novoProduto("P-1", "10.00");

        venda.adicionarItem(produto, new BigDecimal("2.000"));

        produto.alterarValorUnitario(new BigDecimal("15.00"));

        assertEquals(
            new BigDecimal("20.00"),
            venda.calcularTotal()
        );
    }

    @Test
    void deveArredondarSubtotalEParaDuasCasas() {
        Venda venda = novaVenda();
        Produto produto = novoProduto("P-2", "10.00");

        venda.adicionarItem(produto, new BigDecimal("0.333"));

        assertEquals(
            new BigDecimal("3.33"),
            venda.getItens().get(0).calcularSubtotal()
        );

        assertEquals(
            new BigDecimal("3.33"),
            venda.calcularTotal()
        );
    }

    @Test
    void naoDeveAlterarVendaFinalizada() {
        Venda venda = novaVenda();
        venda.adicionarItem(novoProduto("P-3", "10.00"), BigDecimal.ONE);

        venda.finalizar();

        assertEquals(StatusVenda.FINALIZADA, venda.getStatus());
        assertThrows(IllegalStateException.class, venda::cancelar);
    }

    @Test
    void naoDeveFinalizarVendaSemItens() {
        Venda venda = novaVenda();

        assertThrows(IllegalStateException.class, venda::finalizar);
        assertEquals(StatusVenda.ABERTA, venda.getStatus());
    }

    @Test
    void naoDeveAceitarColaboradorSemFuncaoDeVenda() {
        Pessoa pessoaCliente = novoCliente();
        Pessoa pessoaColaborador = novoVendedor("estoque@example.com");

        Colaborador estoquista = new Colaborador(
            pessoaColaborador,
            "EST-1",
            FuncaoColaborador.ESTOQUISTA,
            LocalDate.now()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new Venda(
                new Cliente(pessoaCliente, null),
                estoquista,
                LocalDateTime.now()
            )
        );
    }

    @Test
    void naoDeveAceitarDataDeAberturaNula() {
        Pessoa pessoaCliente = novoCliente();
        Pessoa pessoaVendedor = novoVendedor("vendedor@example.com");

        Cliente cliente = new Cliente(pessoaCliente, null);

        Colaborador vendedor = new Colaborador(
            pessoaVendedor,
            "VEN-1",
            FuncaoColaborador.VENDEDOR,
            LocalDate.now()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new Venda(cliente, vendedor, null)
        );
    }

    private Venda novaVenda() {
        Pessoa pessoaCliente = novoCliente();
        Pessoa pessoaVendedor = novoVendedor("vendedor@example.com");

        return new Venda(
            new Cliente(pessoaCliente, null),
            new Colaborador(
                pessoaVendedor,
                "VEN-1",
                FuncaoColaborador.VENDEDOR,
                LocalDate.now()
            ),
            LocalDateTime.of(2026, 11, 10, 10, 0)
        );
    }

    private Pessoa novoCliente() {
        return new Pessoa(
            "Cliente",
            "cliente@example.com",
            "52998224725",
            LocalDate.now()
        );
    }

    private Pessoa novoVendedor(String email) {
        return new Pessoa(
            "Vendedor",
            email,
            "11144477735",
            LocalDate.now()
        );
    }

    private Produto novoProduto(String codigo, String valor) {
        return new Produto(
            codigo,
            "Produto",
            new BigDecimal("10.000"),
            new BigDecimal(valor),
            LocalDate.now()
        );
    }
}
