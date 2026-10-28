
package com.curso.grossisports.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para cadastrar um fornecedor")
public record FornecedorRequest(

    @Schema(
        description = "Razão social do fornecedor",
        example = "Fornecedor Esportivo LTDA",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Razão social é obrigatória")
    @Size(
        max = 150,
        message = "Razão social deve possuir no máximo 150 caracteres")
    String razaoSocial,

    @Schema(
        description = "CNPJ do fornecedor contendo exatamente 14 dígitos, sem pontuação",
        example = "12345678000199",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(
        regexp = "\\d{14}",
        message = "CNPJ deve possuir 14 dígitos")
    String cnpj

) {
}
