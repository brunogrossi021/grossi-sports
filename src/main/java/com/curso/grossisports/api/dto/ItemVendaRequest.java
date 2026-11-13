package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Dados necessários para adicionar um item à venda")
public record ItemVendaRequest(
    @Schema(
        description = "Identificador do produto",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Produto é obrigatório")
    @Positive(message = "ID do produto deve ser positivo")
    Long produtoId,

    @Schema(
        description = "Quantidade do produto",
        example = "2.000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
    @Digits(integer = 15, fraction = 3, message = "Quantidade deve possuir até 3 casas decimais")
    BigDecimal quantidade
) {}
