package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.StatusVenda;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resumo dos dados de uma venda")
public record VendaResumoResponse(
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
    StatusVenda status
) {}
