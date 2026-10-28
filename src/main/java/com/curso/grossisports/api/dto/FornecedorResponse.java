
package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de um fornecedor retornados pela API")
public record FornecedorResponse(

    @Schema(description = "Identificador único do fornecedor", example = "1")
    Long id,

    @Schema(description = "Razão social do fornecedor", example = "Fornecedor Esportivo LTDA")
    String razaoSocial,

    @Schema(description = "CNPJ do fornecedor, contendo 14 dígitos", example = "12345678000199")
    String cnpj,

    @Schema(description = "Situação atual do fornecedor", example = "ATIVO")
    Status status

) {
}
