package com.curso.grossisports.api.mapper;

import com.curso.grossisports.api.dto.ProdutoRequest;
import com.curso.grossisports.api.dto.ProdutoResponse;
import com.curso.grossisports.domain.Fornecedor;
import com.curso.grossisports.domain.Produto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ProdutoMapper {

    public Produto toEntity(ProdutoRequest request) {
        return new Produto(
            request.codigoBarras(),
            request.descricao(),
            request.saldoEstoque(),
            request.valorUnitario(),
            request.estoqueMinimo(),
            LocalDate.now()
        );
    }

    public ProdutoResponse toResponse(Produto produto) {
        Fornecedor fornecedor = produto.getFornecedor();

        return new ProdutoResponse(
            produto.getId(),
            produto.getCodigoBarras(),
            produto.getDescricao(),
            produto.getSaldoEstoque(),
            produto.getValorUnitario(),
            produto.getEstoqueMinimo(),
            produto.calcularValorEstoque(),
            produto.getDataCadastro(),
            produto.getStatus(),
            produto.getGrupo().getId(),
            produto.getGrupo().getNome(),
            fornecedor == null ? null : fornecedor.getId(),
            fornecedor == null ? null : fornecedor.getRazaoSocial()
        );
    }
}
