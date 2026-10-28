
package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para cadastrar ou editar um grupo de produtos")
public record GrupoProdutoRequest(

    @Schema(
        description = "Nome do grupo de produtos",
        example = "Bolas",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Nome do grupo é obrigatório")
    @Size(
        max = 120,
        message = "Nome do grupo deve possuir no máximo 120 caracteres")
    String nome

) {
}
