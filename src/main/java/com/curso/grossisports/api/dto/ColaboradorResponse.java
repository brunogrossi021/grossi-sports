package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.FuncaoColaborador;
import com.curso.grossisports.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Dados retornados de um colaborador")
public record ColaboradorResponse(
    @Schema(example = "1")
    Long id,
    @Schema(example = "1")
    Long pessoaId,
    @Schema(example = "Bruno Henrique")
    String nome,
    @Schema(example = "bruno@email.com")
    String email,
    @Schema(example = "52998224725")
    String cpf,
    @Schema(example = "ATIVO")
    Status status,
    @Schema(example = "MAT-001")
    String matricula,
    @Schema(example = "VENDEDOR")
    FuncaoColaborador funcao,
    @Schema(example = "2026-01-01")
    LocalDate dataAdmissao
) {}
