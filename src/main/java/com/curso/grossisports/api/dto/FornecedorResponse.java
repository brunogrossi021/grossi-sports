package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.Status;

public record FornecedorResponse(
    Long id,
    String razaoSocial,
    String cnpj,
    Status status
) {
}
