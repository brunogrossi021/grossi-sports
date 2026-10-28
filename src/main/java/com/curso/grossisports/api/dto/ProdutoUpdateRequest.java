
package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados para atualizar um produto esportivo existente")
public record ProdutoUpdateRequest(

    @Schema(
        description = "Nova descrição do produto",
        example = "Bola de futebol profissional",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Descrição é obrigatória")
    @Size(
        max = 150,
        message = "Descrição deve possuir no máximo 150 caracteres")
    String descricao,

    @Schema(
        description = "Novo preço unitário do produto",
        example = "179.90",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Valor unitário é obrigatório")
    @PositiveOrZero(
        message = "Valor unitário não pode ser negativo")
    BigDecimal valorUnitario,

    @Schema(
        description = "Quantidade mínima desejada em estoque",
        example = "5",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Estoque mínimo é obrigatório")
    @PositiveOrZero(
        message = "Estoque mínimo não pode ser negativo")
    BigDecimal estoqueMinimo,

    @Schema(
        description = "Identificador do grupo do produto",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Grupo é obrigatório")
    @Positive(
        message = "Identificador do grupo deve ser positivo")
    Long grupoId,

    @Schema(
        description = "Identificador do fornecedor do produto",
        example = "1"
    )
    @Positive(
        message = "Identificador do fornecedor deve ser positivo")
    Long fornecedorId

) {
}
