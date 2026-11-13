package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de um cliente")
public record ClienteResponse(

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

    @Schema(example = "17999999999")
    String telefone

) {
}
