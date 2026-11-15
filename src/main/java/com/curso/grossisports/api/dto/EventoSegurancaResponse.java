package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.TipoEventoSeguranca;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Evento de segurança exposto na consulta administrativa (Aula 17).
 *
 * <p>O {@code identificadorHash} de correlação <strong>não</strong> é
 * exposto: ele existe para agrupar tentativas internamente e não precisa
 * circular pela API. Nenhum campo carrega senha, token ou login em texto
 * aberto.</p>
 */
@Schema(
    description = "Evento de segurança da trilha de auditoria; não expõe "
        + "o hash de correlação nem segredos"
)
public record EventoSegurancaResponse(

    @Schema(description = "Identificador do evento", example = "42")
    Long id,

    @Schema(description = "Tipo do evento", example = "LOGIN_FALHA")
    TipoEventoSeguranca tipo,

    @Schema(
        description = "Instante em que o evento ocorreu",
        example = "2026-11-15T12:00:00Z"
    )
    Instant ocorridoEm,

    @Schema(
        description = "Conta que iniciou a ação, quando conhecida",
        example = "1"
    )
    Long atorContaId,

    @Schema(
        description = "Conta afetada, quando conhecida",
        example = "2"
    )
    Long alvoContaId,

    @Schema(
        description = "Endereço de origem observado da requisição",
        example = "127.0.0.1"
    )
    String enderecoOrigem

) {
}
