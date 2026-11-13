package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.FuncaoColaborador;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para atualizar o cadastro de um colaborador")
public record ColaboradorAtualizacaoRequest(

    @Schema(
        description = "Matrícula do colaborador",
        example = "MAT-002"
    )
    @Size(
        max = 20,
        message = "Matrícula deve possuir no máximo 20 caracteres"
    )
    String matricula,

    @Schema(
        description = "Função exercida pelo colaborador",
        example = "VENDEDOR"
    )
    FuncaoColaborador funcao

) {
}
