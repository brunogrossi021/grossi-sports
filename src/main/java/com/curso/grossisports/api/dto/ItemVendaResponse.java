package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Dados retornados de um item da venda")
public record ItemVendaResponse(
    @Schema(example = "1")
    Long id,

    @Schema(example = "1")
    Long produtoId,

    @Schema(example = "Chuteira de campo")
    String produtoDescricao,

    @Schema(example = "2.000")
    BigDecimal quantidade,

    @Schema(example = "299.90")
    BigDecimal valorUnitario,

    @Schema(example = "599.80")
    BigDecimal subtotal
) {}
