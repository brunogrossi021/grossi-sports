package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Dados retornados de uma pessoa")
public record PessoaResponse(

    @Schema(description = "Identificador da pessoa", example = "1")
    Long id,

    @Schema(description = "Nome completo", example = "Bruno Henrique")
    String nome,

    @Schema(description = "E-mail", example = "bruno@email.com")
    String email,

    @Schema(description = "CPF com 11 dígitos", example = "52998224725")
    String cpf,

    @Schema(description = "Data de cadastro", example = "2026-10-31")
    LocalDate dataCadastro,

    @Schema(description = "Status da pessoa", example = "ATIVO")
    Status status

) {
}

