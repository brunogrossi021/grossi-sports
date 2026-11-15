package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token opaco emitido no login ou na renovação")
public record RefreshTokenRequest(

    @Schema(
        description = "Refresh token opaco (32 bytes em Base64 URL)",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Refresh token é obrigatório")
    String refreshToken

) {
}
