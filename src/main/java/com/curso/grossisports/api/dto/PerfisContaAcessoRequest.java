package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.PerfilAcesso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

@Schema(
    description = "Perfis de acesso que substituirão os perfis atuais "
        + "da conta. Deve conter ao menos um perfil."
)
public record PerfisContaAcessoRequest(

    @Schema(
        description = "Conjunto de perfis de acesso",
        example = "[\"OPERADOR\", \"ADMINISTRADOR\"]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotEmpty(message = "Informe ao menos um perfil")
    Set<PerfilAcesso> perfis

) {
}
