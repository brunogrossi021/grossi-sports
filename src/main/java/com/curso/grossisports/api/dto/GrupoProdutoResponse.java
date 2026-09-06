package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.Status;

public record GrupoProdutoResponse(
    Long id,
    String nome,
    Status status
) {
}
