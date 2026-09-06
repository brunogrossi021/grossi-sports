package com.curso.grossisports.api.mapper;

import com.curso.grossisports.api.dto.GrupoProdutoRequest;
import com.curso.grossisports.api.dto.GrupoProdutoResponse;
import com.curso.grossisports.domain.GrupoProduto;
import org.springframework.stereotype.Component;

@Component
public class GrupoProdutoMapper {

    public GrupoProduto toEntity(GrupoProdutoRequest request) {
        return new GrupoProduto(request.nome());
    }

    public GrupoProdutoResponse toResponse(GrupoProduto grupo) {
        return new GrupoProdutoResponse(
            grupo.getId(),
            grupo.getNome(),
            grupo.getStatus()
        );
    }
}
