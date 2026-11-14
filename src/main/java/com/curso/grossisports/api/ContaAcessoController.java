package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.AlterarSenhaContaRequest;
import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.ContaAcessoRequest;
import com.curso.grossisports.api.dto.ContaAcessoResponse;
import com.curso.grossisports.api.mapper.ContaAcessoMapper;
import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.service.ContaAcessoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/contas-acesso")
@Tag(
    name = "Contas de acesso",
    description = "Autocadastro didático e dados da conta autenticada"
)
public class ContaAcessoController {

    private final ContaAcessoService service;
    private final ContaAcessoMapper mapper;

    public ContaAcessoController(
        ContaAcessoService service,
        ContaAcessoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Autocadastrar conta",
        description = "Cria a pessoa e a conta de acesso na mesma "
            + "transação, sempre com o perfil OPERADOR. Rota pública "
            + "apenas para fins didáticos de inicialização."
    )
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Conta criada com sucesso"),
        @ApiResponse(
            responseCode = "400",
            description = "Dados, CPF ou senha inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Login, CPF ou e-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping
    public ResponseEntity<ContaAcessoResponse> cadastrar(
        @Valid @RequestBody ContaAcessoRequest request) {

        ContaAcesso conta = service.cadastrar(
            request.nome(),
            request.email(),
            request.cpf(),
            request.login(),
            request.senha()
        );

        URI location = URI.create("/api/contas-acesso/me");

        return ResponseEntity.created(location)
            .body(mapper.toResponse(conta));
    }

    @Operation(
        summary = "Consultar minha conta",
        description = "Retorna os dados da conta autenticada, sem "
            + "expor senha ou hash."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conta retornada"),
        @ApiResponse(
            responseCode = "404",
            description = "Conta de acesso não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping("/me")
    public ContaAcessoResponse buscarAtual(
        Authentication authentication) {

        return mapper.toResponse(
            service.buscarPorLogin(authentication.getName()));
    }

    @Operation(
        summary = "Alterar minha senha",
        description = "Exige a senha atual e grava apenas o novo hash."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Senha alterada"),
        @ApiResponse(
            responseCode = "400",
            description = "Senha atual inválida ou nova senha inválida",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Conta de acesso não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarSenha(
        Authentication authentication,
        @Valid @RequestBody AlterarSenhaContaRequest request) {

        service.alterarSenha(
            authentication.getName(),
            request.senhaAtual(),
            request.novaSenha()
        );

        return ResponseEntity.noContent().build();
    }
}
