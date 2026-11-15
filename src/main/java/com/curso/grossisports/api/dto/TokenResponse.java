package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
    description = "Par de tokens emitido no login e na renovação (Aula 16)"
)
public record TokenResponse(

    @Schema(
        description = "JWT assinado em HS256",
        example = "eyJraWQiOi..."
    )
    String accessToken,

    @Schema(description = "Tipo do token", example = "Bearer")
    String tokenType,

    @Schema(description = "Validade do access token em segundos",
        example = "1800")
    long expiresIn,

    @Schema(
        description = "Instante de expiração do access token",
        example = "2026-10-29T21:30:00Z"
    )
    Instant expiresAt,

    @Schema(
        description = "Refresh token opaco; só existe em claro nesta "
            + "resposta, pois o servidor guarda apenas o hash",
        example = "8fJ2s1kQ..."
    )
    String refreshToken,

    @Schema(
        description = "Limite absoluto da sessão renovável",
        example = "2026-10-30T05:30:00Z"
    )
    Instant refreshTokenExpiresAt

) {
}
