package com.curso.grossisports.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
    name = "Infraestrutura",
    description = "Rotas públicas de verificação da API"
)
public class HealthController {

    @Operation(
        summary = "Verificar saúde da API",
        description = "Rota pública usada para verificar se a API responde."
    )
    @SecurityRequirements
    @GetMapping("/api/health")
    public String health() {
        return "OK";
    }
}
