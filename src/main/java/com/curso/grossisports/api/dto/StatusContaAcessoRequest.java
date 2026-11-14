package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.StatusContaAcesso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description = "Novo status da conta de acesso: ativa ou bloqueada."
)
public record StatusContaAcessoRequest(

    @Schema(
        description = "Status desejado da conta",
        example = "BLOQUEADA",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Status é obrigatório")
    StatusContaAcesso status

) {
}
