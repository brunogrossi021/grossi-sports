package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciais para autenticação e emissão do token")
public record LoginRequest(

    @Schema(
        description = "Login da conta",
        example = "operador.postman",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Login é obrigatório")
    @Size(
        max = 80,
        message = "Login deve possuir no máximo 80 caracteres"
    )
    String login,

    @Schema(
        description = "Senha da conta",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Senha é obrigatória")
    @Size(
        max = 72,
        message = "Senha deve possuir no máximo 72 caracteres"
    )
    String senha

) {
}
