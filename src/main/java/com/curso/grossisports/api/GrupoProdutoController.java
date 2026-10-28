package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.GrupoProdutoRequest;
import com.curso.grossisports.api.dto.GrupoProdutoResponse;
import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.api.mapper.GrupoProdutoMapper;
import com.curso.grossisports.domain.GrupoProduto;
import com.curso.grossisports.service.GrupoProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/grupos-produtos")
@Tag(
    name = "Grupos de produtos",
    description = "Gerenciamento dos grupos de produtos esportivos"
)
public class GrupoProdutoController {

    private final GrupoProdutoService service;
    private final GrupoProdutoMapper mapper;

    public GrupoProdutoController(
        GrupoProdutoService service,
        GrupoProdutoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Cadastrar grupo de produtos",
        description = "Cadastra um novo grupo para classificar produtos esportivos."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Grupo cadastrado com sucesso"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Dados inválidos",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Dados inválidos",
                          "path": "/api/grupos-produtos",
                          "fields": {
                            "nome": "O nome é obrigatório"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Já existe um grupo com esse nome",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Nome de grupo duplicado",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 409,
                          "error": "Conflict",
                          "message": "Já existe um grupo com esse nome",
                          "path": "/api/grupos-produtos",
                          "fields": {}
                        }
                        """
                )
            )
        )
    })
    @PostMapping
    public ResponseEntity<GrupoProdutoResponse> cadastrar(
        @Valid @RequestBody GrupoProdutoRequest request) {

        GrupoProduto grupo = mapper.toEntity(request);
        GrupoProduto cadastrado = service.cadastrar(grupo.getNome());

        URI location = URI.create(
            "/api/grupos-produtos/" + cadastrado.getId());

        return ResponseEntity
            .created(location)
            .body(mapper.toResponse(cadastrado));
    }

    @Operation(
        summary = "Editar grupo de produtos",
        description = "Atualiza o nome de um grupo de produtos existente."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Grupo atualizado com sucesso"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Dados inválidos",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Dados inválidos",
                          "path": "/api/grupos-produtos/1",
                          "fields": {
                            "nome": "O nome é obrigatório"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Grupo não encontrado",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Grupo não encontrado",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Grupo não encontrado",
                          "path": "/api/grupos-produtos/999",
                          "fields": {}
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Já existe um grupo com esse nome",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Nome de grupo duplicado",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 409,
                          "error": "Conflict",
                          "message": "Já existe um grupo com esse nome",
                          "path": "/api/grupos-produtos/1",
                          "fields": {}
                        }
                        """
                )
            )
        )
    })
    @PutMapping("/{id}")
    public GrupoProdutoResponse editar(
        @Parameter(description = "Identificador do grupo", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody GrupoProdutoRequest request) {

        return mapper.toResponse(
            service.editar(id, request.nome()));
    }

    @Operation(
        summary = "Ativar grupo de produtos",
        description = "Ativa um grupo de produtos identificado pelo ID."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Grupo ativado com sucesso"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Grupo não encontrado",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Grupo não encontrado",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Grupo não encontrado",
                          "path": "/api/grupos-produtos/999/ativar",
                          "fields": {}
                        }
                        """
                )
            )
        )
    })
    @PatchMapping("/{id}/ativar")
    public GrupoProdutoResponse ativar(
        @Parameter(description = "Identificador do grupo", example = "1")
        @PathVariable Long id) {

        return mapper.toResponse(
            service.ativar(id));
    }

    @Operation(
        summary = "Inativar grupo de produtos",
        description = "Inativa um grupo de produtos identificado pelo ID."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Grupo inativado com sucesso"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Grupo não encontrado",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Grupo não encontrado",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Grupo não encontrado",
                          "path": "/api/grupos-produtos/999/inativar",
                          "fields": {}
                        }
                        """
                )
            )
        )
    })
    @PatchMapping("/{id}/inativar")
    public GrupoProdutoResponse inativar(
        @Parameter(description = "Identificador do grupo", example = "1")
        @PathVariable Long id) {

        return mapper.toResponse(
            service.inativar(id));
    }

    @Operation(
        summary = "Excluir grupo de produtos",
        description = "Exclui um grupo de produtos, desde que não existam produtos associados."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Grupo excluído com sucesso"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Grupo não encontrado",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Grupo não encontrado",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Grupo não encontrado",
                          "path": "/api/grupos-produtos/999",
                          "fields": {}
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Grupo possui produtos associados",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Grupo possui produtos associados",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 409,
                          "error": "Conflict",
                          "message": "Grupo possui produtos associados",
                          "path": "/api/grupos-produtos/1",
                          "fields": {}
                        }
                        """
                )
            )
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
        @Parameter(description = "Identificador do grupo", example = "1")
        @PathVariable Long id) {

        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Buscar grupo de produtos por ID",
        description = "Retorna os dados de um grupo de produtos específico."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Grupo encontrado"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Grupo não encontrado",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Grupo não encontrado",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Grupo não encontrado",
                          "path": "/api/grupos-produtos/999",
                          "fields": {}
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/{id}")
    public GrupoProdutoResponse buscarPorId(
        @Parameter(description = "Identificador do grupo", example = "1")
        @PathVariable Long id) {

        return mapper.toResponse(
            service.buscarPorId(id));
    }

    @Operation(
        summary = "Listar grupos de produtos",
        description = "Lista os grupos de produtos com paginação e ordenação."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de grupos retornada"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros inválidos",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Parâmetros inválidos",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Parâmetros de paginação ou ordenação inválidos",
                          "path": "/api/grupos-produtos",
                          "fields": {}
                        }
                        """
                )
            )
        )
    })
    @GetMapping
    public PageResponse<GrupoProdutoResponse> listar(
        @Parameter(
            description = "Número da página, começando em zero",
            example = "0"
        )
        @RequestParam(defaultValue = "0") int pagina,

        @Parameter(
            description = "Quantidade de itens por página",
            example = "10"
        )
        @RequestParam(defaultValue = "10") int tamanho,

        @Parameter(
            description = "Campo usado para ordenação",
            example = "nome"
        )
        @RequestParam(defaultValue = "nome") String ordenarPor,

        @Parameter(
            description = "Direção da ordenação: asc ou desc",
            example = "asc"
        )
        @RequestParam(defaultValue = "asc") String direcao) {

        PageResponse<GrupoProduto> resultado =
            service.listarPaginado(pagina, tamanho, ordenarPor, direcao);

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
}
