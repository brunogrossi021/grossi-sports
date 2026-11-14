package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Token de acesso emitido após autenticação")
public record TokenResponse(

    @Schema(
        description = "JWT assinado em HS256",
        example = "eyJraWQiOi..."
    )
    String accessToken,

    @Schema(description = "Tipo do token", example = "Bearer")
    String tokenType,

    @Schema(description = "Validade em segundos", example = "1800")
    long expiresIn,

    @Schema(
        description = "Instante de expiração",
        example = "2026-10-29T21:30:00Z"
    )
    Instant expiresAt

) {
}
