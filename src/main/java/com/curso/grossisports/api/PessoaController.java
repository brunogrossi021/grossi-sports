package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.api.dto.PessoaRequest;
import com.curso.grossisports.api.dto.PessoaResponse;
import com.curso.grossisports.api.dto.StatusRequest;
import com.curso.grossisports.api.mapper.PessoaMapper;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.Status;
import com.curso.grossisports.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/pessoas")
@Tag(
    name = "Pessoas",
    description = "Identidade civil compartilhada pelos papéis comerciais"
)
public class PessoaController {

    private final PessoaService service;
    private final PessoaMapper mapper;

    public PessoaController(
        PessoaService service,
        PessoaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Cadastrar pessoa",
        description = "Cadastra uma pessoa com nome, e-mail e CPF."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pessoa cadastrada com sucesso"),
        @ApiResponse(
            responseCode = "400",
            description = "Dados ou CPF inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "CPF ou e-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping
    public ResponseEntity<PessoaResponse> cadastrar(
        @Valid @RequestBody PessoaRequest request) {

        Pessoa cadastrada = service.cadastrar(
            request.nome(),
            request.email(),
            request.cpf()
        );

        URI location = URI.create(
            "/api/pessoas/" + cadastrada.getId()
        );

        return ResponseEntity.created(location)
            .body(mapper.toResponse(cadastrada));
    }

    @Operation(
        summary = "Pesquisar pessoas",
        description = "Lista pessoas com paginação, ordenação e filtros opcionais."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de pessoas retornada"),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping
    public PageResponse<PessoaResponse> pesquisar(
        @Parameter(description = "Número da página, começando em zero", example = "0")
        @RequestParam(defaultValue = "0") int pagina,

        @Parameter(description = "Quantidade de itens por página, de 1 a 100", example = "10")
        @RequestParam(defaultValue = "10") int tamanho,

        @Parameter(description = "Campo usado para ordenação", example = "nome")
        @RequestParam(defaultValue = "nome") String ordenarPor,

        @Parameter(description = "Direção da ordenação: asc ou desc", example = "asc")
        @RequestParam(defaultValue = "asc") String direcao,

        @Parameter(description = "Filtra pelo nome da pessoa", example = "Bruno")
        @RequestParam(required = false) String nome,

        @Parameter(description = "Filtra pelo CPF", example = "52998224725")
        @RequestParam(required = false) String cpf,

        @Parameter(description = "Filtra pela situação da pessoa", example = "ATIVO")
        @RequestParam(required = false) Status status
    ) {
        PageResponse<Pessoa> resultado = service.pesquisar(
            nome,
            cpf,
            status,
            pagina,
            tamanho,
            ordenarPor,
            direcao
        );

        return new PageResponse<>(
            resultado.content().stream()
                .map(mapper::toResponse)
                .toList(),
            resultado.page(),
            resultado.size(),
            resultado.totalElements(),
            resultado.totalPages()
        );
    }

    @Operation(
        summary = "Buscar pessoa por ID",
        description = "Retorna uma pessoa pelo seu identificador."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pessoa encontrada"),
        @ApiResponse(
            responseCode = "404",
            description = "Pessoa não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping("/{id}")
    public PessoaResponse buscarPorId(
        @Parameter(description = "Identificador da pessoa", example = "1")
        @PathVariable Long id) {

        return mapper.toResponse(service.buscarPorId(id));
    }

    @Operation(
        summary = "Alterar pessoa",
        description = "Atualiza o nome, o e-mail e o CPF. Os papéis comerciais são mantidos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pessoa atualizada"),
        @ApiResponse(
            responseCode = "400",
            description = "Dados ou CPF inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pessoa não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "CPF ou e-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PutMapping("/{id}")
    public PessoaResponse alterar(
        @Parameter(description = "Identificador da pessoa", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody PessoaRequest request) {

        Pessoa alterada = service.alterar(
            id,
            request.nome(),
            request.email(),
            request.cpf()
        );

        return mapper.toResponse(alterada);
    }

    @Operation(
        summary = "Alterar status da pessoa",
        description = "Ativa ou inativa uma pessoa cadastrada."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status alterado"),
        @ApiResponse(
            responseCode = "400",
            description = "Status inválido",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pessoa não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PutMapping("/{id}/status")
    public PessoaResponse alterarStatus(
        @Parameter(description = "Identificador da pessoa", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody StatusRequest request) {

        return mapper.toResponse(
            service.alterarStatus(id, request.status()));
    }

    @Operation(
        summary = "Ativar pessoa",
        description = "Ativa uma pessoa cadastrada."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pessoa ativada"),
        @ApiResponse(
            responseCode = "404",
            description = "Pessoa não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PatchMapping("/{id}/ativar")
    public PessoaResponse ativar(
        @Parameter(description = "Identificador da pessoa", example = "1")
        @PathVariable Long id) {

        return mapper.toResponse(service.ativar(id));
    }

    @Operation(
        summary = "Inativar pessoa",
        description = "Inativa uma pessoa cadastrada."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pessoa inativada"),
        @ApiResponse(
            responseCode = "404",
            description = "Pessoa não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PatchMapping("/{id}/inativar")
    public PessoaResponse inativar(
        @Parameter(description = "Identificador da pessoa", example = "1")
        @PathVariable Long id) {

        return mapper.toResponse(service.inativar(id));
    }
}
