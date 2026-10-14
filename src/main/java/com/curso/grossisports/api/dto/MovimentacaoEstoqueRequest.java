package com.curso.grossisports.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MovimentacaoEstoqueRequest(
    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
    @Digits(integer = 15, fraction = 3, message = "Quantidade deve ter no máximo três casas decimais")
    BigDecimal quantidade
) {
}
