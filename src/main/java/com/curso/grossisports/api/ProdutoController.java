package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.MovimentacaoEstoqueRequest;
import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.api.dto.ProdutoRequest;
import com.curso.grossisports.api.dto.ProdutoResponse;
import com.curso.grossisports.api.dto.ProdutoUpdateRequest;
import com.curso.grossisports.api.mapper.ProdutoMapper;
import com.curso.grossisports.domain.Produto;
import com.curso.grossisports.domain.Status;
import com.curso.grossisports.service.ProdutoService;
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

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/produtos")
@Tag(name = "Produtos", description = "Gerenciamento de produtos esportivos e movimentações de estoque")
public class ProdutoController {

    private final ProdutoService service;
    private final ProdutoMapper mapper;

    public ProdutoController(ProdutoService service, ProdutoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Cadastrar produto",
        description = "Cadastra um produto esportivo e associa seu grupo e fornecedor."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Produto cadastrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Grupo ou fornecedor não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Código de barras já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ProdutoResponse> cadastrar(@Valid @RequestBody ProdutoRequest request) {
        Produto produto = mapper.toEntity(request);
        Produto cadastrado = service.cadastrar(produto, request.grupoId(), request.fornecedorId());
        URI location = URI.create("/api/produtos/" + cadastrado.getId());
        return ResponseEntity.created(location).body(mapper.toResponse(cadastrado));
    }

    @Operation(
        summary = "Editar produto",
        description = "Atualiza os dados de um produto existente."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Produto, grupo ou fornecedor não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Código de barras já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponse> editar(
        @Parameter(description = "Identificador do produto", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody ProdutoUpdateRequest request) {

        Produto produto = service.editar(id, request);
        return ResponseEntity.ok(mapper.toResponse(produto));
    }

    @Operation(
        summary = "Buscar produto por ID",
        description = "Retorna os dados de um produto específico."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto encontrado"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ProdutoResponse buscarPorId(
        @Parameter(description = "Identificador do produto", example = "1")
        @PathVariable Long id) {

        return mapper.toResponse(service.buscarPorId(id));
    }

    @Operation(
        summary = "Listar produtos",
        description = "Lista produtos com paginação, ordenação e filtros."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de produtos retornada"),
        @ApiResponse(responseCode = "400", description = "Parâmetros inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public PageResponse<ProdutoResponse> listar(
        @Parameter(description = "Número da página, começando em zero", example = "0")
        @RequestParam(defaultValue = "0") int pagina,

        @Parameter(description = "Quantidade de itens por página, de 1 a 100", example = "10")
        @RequestParam(defaultValue = "10") int tamanho,

        @Parameter(description = "Campo usado para ordenação", example = "descricao")
        @RequestParam(defaultValue = "descricao") String ordenarPor,

        @Parameter(description = "Direção da ordenação: asc ou desc", example = "asc")
        @RequestParam(defaultValue = "asc") String direcao,

        @Parameter(description = "Filtra pela descrição do produto", example = "Bola")
        @RequestParam(required = false) String descricao,

        @Parameter(description = "Filtra pelo código de barras", example = "7891234567890")
        @RequestParam(required = false) String codigoBarras,

        @Parameter(description = "Filtra pelo identificador do grupo", example = "1")
        @RequestParam(required = false) Long grupoId,

        @Parameter(description = "Filtra pela situação do produto", example = "ATIVO")
        @RequestParam(required = false) Status status,

        @Parameter(description = "Preço unitário mínimo", example = "10.00")
        @RequestParam(required = false) BigDecimal valorMinimo,

        @Parameter(description = "Preço unitário máximo", example = "200.00")
        @RequestParam(required = false) BigDecimal valorMaximo) {

        PageResponse<Produto> resultado = service.listarPaginado(
            pagina, tamanho, ordenarPor, direcao, descricao,
            codigoBarras, grupoId, status, valorMinimo, valorMaximo);

        return new PageResponse<>(
            resultado.content().stream().map(mapper::toResponse).toList(),
            resultado.page(),
            resultado.size(),
            resultado.totalElements(),
            resultado.totalPages());
    }

    @Operation(
        summary = "Registrar entrada de estoque",
        description = "Adiciona uma quantidade ao estoque do produto informado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrada registrada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Quantidade inválida",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/estoque/entradas")
    public ProdutoResponse receberEstoque(
        @Parameter(description = "Identificador do produto", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody MovimentacaoEstoqueRequest request) {

        Produto produto = service.receberEstoque(id, request.quantidade());
        return mapper.toResponse(produto);
    }

    @Operation(
        summary = "Registrar saída de estoque",
        description = "Retira uma quantidade do estoque do produto informado, respeitando as regras de estoque."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Saída registrada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Quantidade inválida ou saldo insuficiente",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/estoque/saidas")
    public ProdutoResponse retirarEstoque(
        @Parameter(description = "Identificador do produto", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody MovimentacaoEstoqueRequest request) {

        Produto produto = service.retirarEstoque(id, request.quantidade());
        return mapper.toResponse(produto);
    }

    @Operation(
        summary = "Ativar produto",
        description = "Ativa o produto identificado pelo ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto ativado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/ativar")
    public ProdutoResponse ativar(
        @Parameter(description = "Identificador do produto", example = "1")
        @PathVariable Long id) {

        return mapper.toResponse(service.ativar(id));
    }

    @Operation(
        summary = "Inativar produto",
        description = "Inativa o produto identificado pelo ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto inativado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/inativar")
    public ProdutoResponse inativar(
        @Parameter(description = "Identificador do produto", example = "1")
        @PathVariable Long id) {

        return mapper.toResponse(service.inativar(id));
    }
}
