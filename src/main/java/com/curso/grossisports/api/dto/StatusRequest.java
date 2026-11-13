package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados necessários para alterar o status de uma pessoa")
public record StatusRequest(

    @Schema(
        description = "Novo status da pessoa",
        example = "ATIVO",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Status é obrigatório")
    Status status

) {
}
