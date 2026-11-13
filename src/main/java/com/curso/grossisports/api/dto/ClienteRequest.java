package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para cadastrar um cliente")
public record ClienteRequest(

    @Schema(
        description = "Identificador da pessoa associada ao cliente",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Pessoa é obrigatória")
    @Positive(message = "ID da pessoa deve ser positivo")
    Long pessoaId,

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
