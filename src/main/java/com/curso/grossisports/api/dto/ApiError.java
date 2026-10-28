package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(
    description = "Detalhes de uma resposta de erro da API",
    example = """
        {
          "timestamp": "2026-10-22T18:30:00Z",
          "status": 404,
          "error": "Not Found",
          "message": "Produto não encontrado",
          "path": "/api/produtos/999",
          "fields": {}
        }
        """
)
public record ApiError(

    @Schema(
        description = "Data e hora em que o erro ocorreu",
        example = "2026-10-22T18:30:00Z"
    )
    Instant timestamp,

    @Schema(
        description = "Código HTTP do erro",
        example = "404"
    )
    int status,

    @Schema(
        description = "Descrição do tipo de erro HTTP",
        example = "Not Found"
    )
    String error,

    @Schema(
        description = "Mensagem explicando o erro",
        example = "Produto não encontrado"
    )
    String message,

    @Schema(
        description = "Caminho da requisição que gerou o erro",
        example = "/api/produtos/999"
    )
    String path,

    @Schema(
        description = "Detalhes dos campos inválidos, quando aplicável",
        example = """
            {
              "descricao": "A descrição é obrigatória",
              "valorUnitario": "Deve ser maior que zero"
            }
            """
    )
    Map<String, String> fields

) {
}
