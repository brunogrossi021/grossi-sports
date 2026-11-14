package com.curso.grossisports.api.dto;

import com.curso.grossisports.validation.CpfValido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
    description = "Dados para a criação administrativa: cria a pessoa "
        + "e a conta de acesso na mesma requisição, sempre com o perfil "
        + "OPERADOR. Operação restrita a ADMINISTRADOR. Não aceita "
        + "pessoa existente nem perfil de acesso."
)
public record ContaAcessoRequest(

    @Schema(
        description = "Nome completo da pessoa",
        example = "Ana Souza",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Nome é obrigatório")
    @Size(
        max = 150,
        message = "Nome deve possuir no máximo 150 caracteres"
    )
    String nome,

    @Schema(
        description = "E-mail da pessoa",
        example = "ana.souza@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(
        max = 180,
        message = "E-mail deve possuir no máximo 180 caracteres"
    )
    String email,

    @Schema(
        description = "CPF contendo exatamente 11 dígitos, sem pontuação",
        example = "52998224725",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "CPF é obrigatório")
    @Pattern(
        regexp = "\\d{11}",
        message = "CPF deve possuir 11 dígitos"
    )
    @CpfValido
    String cpf,

    @Schema(
        description = "Login de acesso, único e sem diferenciar maiúsculas",
        example = "ana.souza",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Login é obrigatório")
    @Size(
        min = 3,
        max = 80,
        message = "Login deve possuir entre 3 e 80 caracteres"
    )
    @Pattern(
        regexp = "^[A-Za-z0-9._-]+$",
        message = "Login aceita somente letras, números, ponto, hífen e sublinhado"
    )
    String login,

    @Schema(
        description = "Senha com no mínimo 12 caracteres e no máximo 72 bytes em UTF-8",
        example = "uma-frase-senha-2026",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Senha é obrigatória")
    @Size(
        min = 12,
        max = 72,
        message = "Senha deve possuir entre 12 e 72 caracteres"
    )
    String senha

) {
}
