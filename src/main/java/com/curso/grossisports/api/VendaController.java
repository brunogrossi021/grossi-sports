package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.ItemVendaRequest;
import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.api.dto.VendaRequest;
import com.curso.grossisports.api.dto.VendaResponse;
import com.curso.grossisports.api.dto.VendaResumoResponse;
import com.curso.grossisports.api.mapper.VendaMapper;
import com.curso.grossisports.domain.StatusVenda;
import com.curso.grossisports.domain.Venda;
import com.curso.grossisports.service.VendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/vendas")
@Tag(name = "Vendas", description = "Gerenciamento de vendas e itens")
public class VendaController {

    private final VendaService service;
    private final VendaMapper mapper;

    public VendaController(VendaService service, VendaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Abrir venda",
        description = "Cria uma venda ABERTA para um cliente e um vendedor ativos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Venda aberta com sucesso"),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cliente ou colaborador não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping
    public ResponseEntity<VendaResponse> abrirVenda(
        @Valid @RequestBody VendaRequest request
    ) {
        Venda venda = service.abrirVenda(
            request.clienteId(),
            request.vendedorId()
        );

        URI location = URI.create("/api/vendas/" + venda.getId());

        return ResponseEntity.created(location)
            .body(mapper.toResponse(venda));
    }

    @Operation(
        summary = "Consultar venda",
        description = "Retorna os dados completos de uma venda, incluindo seus itens."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venda encontrada"),
        @ApiResponse(
            responseCode = "404",
            description = "Venda não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping("/{id}")
    public VendaResponse buscarPorId(
        @Parameter(description = "Identificador da venda", example = "1")
        @PathVariable Long id
    ) {
        return mapper.toResponse(service.buscarPorId(id));
    }

    @Operation(
        summary = "Pesquisar vendas",
        description = "Lista resumos de vendas filtrando por status, participantes e intervalo inclusivo de datas."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de vendas retornada"),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping
    public PageResponse<VendaResumoResponse> pesquisar(
        @Parameter(description = "Número da página, começando em zero", example = "0")
        @RequestParam(defaultValue = "0") int pagina,

        @Parameter(description = "Quantidade de vendas por página", example = "10")
        @RequestParam(defaultValue = "10") int tamanho,

        @Parameter(description = "Campo usado para ordenação", example = "dataAbertura")
        @RequestParam(defaultValue = "dataAbertura") String ordenarPor,

        @Parameter(description = "Direção da ordenação: asc ou desc", example = "desc")
        @RequestParam(defaultValue = "desc") String direcao,

        @Parameter(description = "Filtra pelo status da venda", example = "ABERTA")
        @RequestParam(required = false) StatusVenda status,

        @Parameter(description = "Filtra pelo identificador do cliente", example = "1")
        @RequestParam(required = false) Long clienteId,

        @Parameter(description = "Filtra pelo identificador do vendedor", example = "1")
        @RequestParam(required = false) Long vendedorId,

        @Parameter(description = "Data inicial do período, inclusive", example = "2026-11-01")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,

        @Parameter(description = "Data final do período, inclusive", example = "2026-11-30")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal
    ) {
        PageResponse<Venda> resultado = service.pesquisar(
            status,
            clienteId,
            vendedorId,
            dataInicial,
            dataFinal,
            pagina,
            tamanho,
            ordenarPor,
            direcao
        );

        return new PageResponse<>(
            resultado.content().stream()
                .map(mapper::toResumoResponse)
                .toList(),
            resultado.page(),
            resultado.size(),
            resultado.totalElements(),
            resultado.totalPages()
        );
    }

    @Operation(
        summary = "Adicionar item à venda",
        description = "Captura o preço atual do produto; somente vendas abertas aceitam itens."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item adicionado com sucesso"),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos ou venda não está aberta",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Venda ou produto não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping("/{id}/itens")
    public VendaResponse adicionarItem(
        @Parameter(description = "Identificador da venda", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody ItemVendaRequest request
    ) {
        Venda venda = service.adicionarItem(
            id,
            request.produtoId(),
            request.quantidade()
        );

        return mapper.toResponse(venda);
    }

    @Operation(
        summary = "Remover item da venda",
        description = "Remove um item de uma venda que ainda esteja aberta."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item removido com sucesso"),
        @ApiResponse(
            responseCode = "400",
            description = "Item não encontrado ou venda não está aberta",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Venda não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @DeleteMapping("/{vendaId}/itens/{itemId}")
    public VendaResponse removerItem(
        @Parameter(description = "Identificador da venda", example = "1")
        @PathVariable Long vendaId,

        @Parameter(description = "Identificador do item", example = "1")
        @PathVariable Long itemId
    ) {
        return mapper.toResponse(service.removerItem(vendaId, itemId));
    }

    @Operation(
        summary = "Finalizar venda",
        description = "Valida toda a venda, bloqueia os produtos em ordem determinística e baixa o estoque de todos os itens na mesma transação."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venda finalizada com sucesso"),
        @ApiResponse(
            responseCode = "400",
            description = "Venda vazia, estoque insuficiente ou estado inválido",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Venda ou produto não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping("/{id}/finalizacao")
    public VendaResponse finalizar(
        @Parameter(description = "Identificador da venda", example = "1")
        @PathVariable Long id
    ) {
        return mapper.toResponse(service.finalizar(id));
    }

    @Operation(
        summary = "Cancelar venda",
        description = "Cancela uma venda ainda aberta; como não houve baixa, não há estorno de estoque."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venda cancelada com sucesso"),
        @ApiResponse(
            responseCode = "400",
            description = "Venda não está aberta",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Venda não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping("/{id}/cancelamento")
    public VendaResponse cancelar(
        @Parameter(description = "Identificador da venda", example = "1")
        @PathVariable Long id
    ) {
        return mapper.toResponse(service.cancelar(id));
    }
}
