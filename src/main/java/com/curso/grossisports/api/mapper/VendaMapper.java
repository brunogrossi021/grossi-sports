package com.curso.grossisports.api.mapper;

import com.curso.grossisports.api.dto.ItemVendaResponse;
import com.curso.grossisports.api.dto.VendaResponse;
import com.curso.grossisports.api.dto.VendaResumoResponse;
import com.curso.grossisports.domain.Venda;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VendaMapper {

    public VendaResponse toResponse(Venda venda) {
        List<ItemVendaResponse> itens = venda.getItens()
            .stream()
            .map(item -> new ItemVendaResponse(
                item.getId(),
                item.getProduto().getId(),
                item.getProduto().getDescricao(),
                item.getQuantidade(),
                item.getValorUnitario(),
                item.calcularSubtotal()
            ))
            .toList();

        return new VendaResponse(
            venda.getId(),
            venda.getCliente().getId(),
            venda.getCliente().getPessoa().getNome(),
            venda.getColaborador().getId(),
            venda.getColaborador().getPessoa().getNome(),
            venda.getDataAbertura(),
            venda.getStatus(),
            itens,
            venda.calcularTotal()
        );
    }

    public VendaResumoResponse toResumoResponse(Venda venda) {
        return new VendaResumoResponse(
            venda.getId(),
            venda.getCliente().getId(),
            venda.getCliente().getPessoa().getNome(),
            venda.getColaborador().getId(),
            venda.getColaborador().getPessoa().getNome(),
            venda.getDataAbertura(),
            venda.getStatus()
        );
    }
}
