package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Dados necessários para abrir uma venda")
public record VendaRequest(
    @Schema(
        description = "Identificador do cliente",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Cliente é obrigatório")
    @Positive(message = "ID do cliente deve ser positivo")
    Long clienteId,

    @Schema(
        description = "Identificador do colaborador vendedor",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Vendedor é obrigatório")
    @Positive(message = "ID do vendedor deve ser positivo")
    Long vendedorId
) {}
