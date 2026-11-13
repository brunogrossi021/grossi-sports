package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.StatusVenda;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Dados completos de uma venda")
public record VendaResponse(
    @Schema(example = "1")
    Long id,

    @Schema(example = "1")
    Long clienteId,

    @Schema(example = "Bruno Henrique")
    String clienteNome,

    @Schema(example = "1")
    Long vendedorId,

    @Schema(example = "Carlos Silva")
    String vendedorNome,

    @Schema(example = "2026-11-04T20:00:00")
    LocalDateTime dataVenda,

    @Schema(example = "ABERTA")
    StatusVenda status,

    List<ItemVendaResponse> itens,

    @Schema(example = "599.80")
    BigDecimal total
) {}
