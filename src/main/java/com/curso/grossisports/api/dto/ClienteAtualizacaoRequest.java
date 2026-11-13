package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para atualizar o cadastro de um cliente")
public record ClienteAtualizacaoRequest(

    @Schema(
        description = "Telefone de contato do cliente",
        example = "17999999999"
    )
    @Size(
        max = 30,
        message = "Telefone deve possuir no máximo 30 caracteres"
    )
    String telefone

) {
}
