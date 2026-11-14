package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para alterar a senha da própria conta")
public record AlterarSenhaContaRequest(

    @Schema(
        description = "Senha atual da conta",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Senha atual é obrigatória")
    String senhaAtual,

    @Schema(
        description = "Nova senha com no mínimo 12 caracteres e no máximo 72 bytes em UTF-8",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Nova senha é obrigatória")
    @Size(
        min = 12,
        max = 72,
        message = "Nova senha deve possuir entre 12 e 72 caracteres"
    )
    String novaSenha

) {
}
