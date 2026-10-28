
package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados necessários para cadastrar um produto esportivo")
public record ProdutoRequest(

    @Schema(description = "Código de barras do produto", example = "7891234567890", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Código de barras é obrigatório")
    @Size(
        max = 50,
        message = "Código de barras deve possuir no máximo 50 caracteres")
    String codigoBarras,

    @Schema(description = "Descrição ou nome do produto", example = "Bola de futebol")
    @NotBlank(message = "Descrição é obrigatória")
    @Size(
        max = 150,
        message = "Descrição deve possuir no máximo 150 caracteres")
    String descricao,

    @Schema(description = "Quantidade inicial disponível em estoque", example = "20")
    @NotNull(message = "Saldo de estoque é obrigatório")
    @PositiveOrZero(
        message = "Saldo de estoque não pode ser negativo")
    BigDecimal saldoEstoque,

    @Schema(description = "Preço unitário do produto", example = "149.90")
    @NotNull(message = "Valor unitário é obrigatório")
    @PositiveOrZero(
        message = "Valor unitário não pode ser negativo")
    BigDecimal valorUnitario,

    @Schema(description = "Quantidade mínima desejada em estoque", example = "5")
    @NotNull(message = "Estoque mínimo é obrigatório")
    @PositiveOrZero(
        message = "Estoque mínimo não pode ser negativo")
    BigDecimal estoqueMinimo,

    @Schema(description = "Identificador do grupo ao qual o produto pertence", example = "1")
    @NotNull(message = "Grupo é obrigatório")
    @Positive(
        message = "Identificador do grupo deve ser positivo")
    Long grupoId,

    @Schema(description = "Identificador do fornecedor do produto", example = "1")
    @Positive(
        message = "Identificador do fornecedor deve ser positivo")
    Long fornecedorId

) {
}
