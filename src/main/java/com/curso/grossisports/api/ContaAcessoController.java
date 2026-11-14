package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.AlterarSenhaContaRequest;
import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.ContaAcessoRequest;
import com.curso.grossisports.api.dto.ContaAcessoResponse;
import com.curso.grossisports.api.dto.PerfisContaAcessoRequest;
import com.curso.grossisports.api.dto.StatusContaAcessoRequest;
import com.curso.grossisports.api.mapper.ContaAcessoMapper;
import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.service.ContaAcessoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    description = "Criação e administração de contas (ADMINISTRADOR) "
        + "e dados da conta autenticada"
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
        summary = "Cadastrar conta",
        description = "Cria a pessoa e a conta de acesso na mesma "
            + "transação, sempre com o perfil OPERADOR. Operação "
            + "restrita a ADMINISTRADOR. O cadastro público foi "
            + "encerrado na Aula 14."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Conta criada com sucesso"),
        @ApiResponse(
            responseCode = "400",
            description = "Dados, CPF ou senha inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Sem token válido",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sem perfil ADMINISTRADOR",
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

        URI location = URI.create(
            "/api/contas-acesso/" + conta.getId());

        return ResponseEntity.created(location)
            .body(mapper.toResponse(conta));
    }

    @Operation(
        summary = "Alterar perfis da conta",
        description = "Substitui os perfis de acesso. Restrito a "
            + "ADMINISTRADOR. Não permite deixar a conta sem perfil, "
            + "remover o próprio perfil administrativo nem remover o "
            + "perfil do último administrador ativo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfis atualizados"),
        @ApiResponse(
            responseCode = "400",
            description = "Perfis inválidos ou invariante administrativa violada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Sem token válido",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sem perfil ADMINISTRADOR",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Conta de acesso não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PutMapping("/{id}/perfis")
    public ContaAcessoResponse alterarPerfis(
        @Parameter(description = "Identificador da conta", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody PerfisContaAcessoRequest request,
        Authentication authentication) {

        return mapper.toResponse(service.alterarPerfis(
            id,
            request.perfis(),
            authentication.getName()
        ));
    }

    @Operation(
        summary = "Ativar ou bloquear conta",
        description = "Ativa ou bloqueia a conta. Restrito a "
            + "ADMINISTRADOR. Não permite bloquear a própria conta nem "
            + "o último administrador ativo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status alterado"),
        @ApiResponse(
            responseCode = "400",
            description = "Status inválido ou invariante administrativa violada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Sem token válido",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sem perfil ADMINISTRADOR",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Conta de acesso não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PutMapping("/{id}/status")
    public ContaAcessoResponse alterarStatus(
        @Parameter(description = "Identificador da conta", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody StatusContaAcessoRequest request,
        Authentication authentication) {

        return mapper.toResponse(service.alterarStatus(
            id,
            request.status(),
            authentication.getName()
        ));
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
