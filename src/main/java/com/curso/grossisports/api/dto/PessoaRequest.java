package com.curso.grossisports.api.dto;

import com.curso.grossisports.validation.CpfValido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para cadastrar uma pessoa")
public record PessoaRequest(

    @Schema(
        description = "Nome completo da pessoa",
        example = "Bruno Henrique",
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
        example = "bruno@email.com",
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
    String cpf

) {
}
