package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.PerfilAcesso;
import com.curso.grossisports.domain.StatusContaAcesso;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(
    description = "Conta autenticável; nunca contém senha ou hash"
)
public record ContaAcessoResponse(

    @Schema(description = "Identificador da conta", example = "1")
    Long id,

    @Schema(description = "Identificador da pessoa vinculada", example = "1")
    Long pessoaId,

    @Schema(description = "Nome da pessoa vinculada", example = "Ana Souza")
    String nomePessoa,

    @Schema(description = "Login de acesso", example = "ana.souza")
    String login,

    @Schema(description = "Status da conta", example = "ATIVA")
    StatusContaAcesso status,

    @Schema(description = "Perfis de acesso concedidos", example = "[\"OPERADOR\"]")
    Set<PerfilAcesso> perfis

) {
}
