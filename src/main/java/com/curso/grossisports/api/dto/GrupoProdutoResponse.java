
package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de um grupo de produtos retornados pela API")
public record GrupoProdutoResponse(

    @Schema(description = "Identificador único do grupo", example = "1")
    Long id,

    @Schema(description = "Nome do grupo de produtos", example = "Bolas")
    String nome,

    @Schema(description = "Situação atual do grupo", example = "ATIVO")
    Status status

) {
}
