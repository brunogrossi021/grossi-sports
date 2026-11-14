package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.LoginRequest;
import com.curso.grossisports.api.dto.TokenResponse;
import com.curso.grossisports.security.TokenAcesso;
import com.curso.grossisports.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autenticacao")
@Tag(
    name = "Autenticação",
    description = "Login e emissão de token JWT"
)
public class AutenticacaoController {

    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    @Operation(
        summary = "Realizar login",
        description = "Valida login e senha e emite um JWT com validade "
            + "de 30 minutos. Rota pública: o cliente ainda não possui "
            + "token. Credencial inválida retorna 401 genérico."
    )
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token emitido"),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Login ou senha inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        TokenAcesso token = service.autenticar(
            request.login(), request.senha());

        return new TokenResponse(
            token.valor(),
            "Bearer",
            token.duracaoSegundos(),
            token.expiraEm());
    }
}
