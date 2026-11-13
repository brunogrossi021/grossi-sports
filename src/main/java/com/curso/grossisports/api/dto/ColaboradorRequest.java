package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.FuncaoColaborador;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Dados necessários para cadastrar um colaborador")
public record ColaboradorRequest(

    @Schema(
        description = "Identificador da pessoa associada ao colaborador",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Pessoa é obrigatória")
    @Positive(message = "ID da pessoa deve ser positivo")
    Long pessoaId,

    @Schema(
        description = "Matrícula do colaborador",
        example = "MAT-001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Matrícula é obrigatória")
    @Size(
        max = 20,
        message = "Matrícula deve possuir no máximo 20 caracteres"
    )
    String matricula,

    @Schema(
        description = "Função exercida pelo colaborador",
        example = "VENDEDOR",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Função é obrigatória")
    FuncaoColaborador funcao,

    @Schema(
        description = "Data de admissão do colaborador",
        example = "2026-01-01",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Data de admissão é obrigatória")
    LocalDate dataAdmissao

) {
}
