package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.LoginRequest;
import com.curso.grossisports.api.dto.RefreshTokenRequest;
import com.curso.grossisports.api.dto.TokenResponse;
import com.curso.grossisports.security.TokenAcesso;
import com.curso.grossisports.security.TokensAutenticacao;
import com.curso.grossisports.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autenticacao")
@Tag(
    name = "Autenticação",
    description = "Login, renovação e encerramento de sessões"
)
public class AutenticacaoController {

    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    @Operation(
        summary = "Realizar login",
        description = "Valida login e senha e emite o par de tokens: um "
            + "access token JWT de 30 minutos e um refresh token opaco "
            + "de longa duração. Rota pública: o cliente ainda não possui "
            + "token. Credencial inválida retorna 401 genérico."
    )
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens emitidos"),
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
        return resposta(service.autenticar(
            request.login(), request.senha()));
    }

    @Operation(
        summary = "Renovar tokens",
        description = "Recebe o refresh token, valida a sessão e gira o "
            + "token: devolve um novo access token e um novo refresh "
            + "token. Reutilizar um refresh token já trocado revoga a "
            + "família inteira e retorna 401 genérico. Rota pública."
    )
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Tokens renovados"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Refresh token inválido ou expirado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping("/refresh")
    public TokenResponse refresh(
        @Valid @RequestBody RefreshTokenRequest request) {

        return resposta(service.renovar(request.refreshToken()));
    }

    @Operation(
        summary = "Encerrar sessão (logout local)",
        description = "Revoga a família de refresh tokens identificada "
            + "pelo token informado. Rota pública e idempotente: "
            + "responde 204 mesmo para token desconhecido. Não invalida "
            + "access tokens já emitidos, que seguem válidos até expirar."
    )
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Sessão encerrada"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        service.encerrarSessao(request.refreshToken());
    }

    @Operation(
        summary = "Encerrar todas as sessões (logout global)",
        description = "Exige Bearer Token. Revoga todas as sessões "
            + "renováveis da conta autenticada. Responde 204 e não "
            + "invalida access tokens já emitidos, que seguem válidos "
            + "até expirar."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Sessões encerradas"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token ausente ou inválido"
        )
    })
    @PostMapping("/logout-todos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutTodos(Authentication authentication) {
        service.encerrarTodasSessoes(authentication.getName());
    }

    private TokenResponse resposta(TokensAutenticacao tokens) {
        TokenAcesso acesso = tokens.acesso();

        return new TokenResponse(
            acesso.valor(),
            "Bearer",
            acesso.duracaoSegundos(),
            acesso.expiraEm(),
            tokens.refreshToken(),
            tokens.refreshTokenExpiraEm());
    }
}
