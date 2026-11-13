package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.ClienteAtualizacaoRequest;
import com.curso.grossisports.api.dto.ClienteRequest;
import com.curso.grossisports.api.dto.ClienteResponse;
import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.api.mapper.ClienteMapper;
import com.curso.grossisports.domain.Cliente;
import com.curso.grossisports.domain.Status;
import com.curso.grossisports.service.ClienteService;
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
@RequestMapping("/api/clientes")
@Tag(
    name = "Clientes",
    description = "Papel comercial associado a uma pessoa"
)
public class ClienteController {

    private final ClienteService service;
    private final ClienteMapper mapper;

    public ClienteController(
        ClienteService service,
        ClienteMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Cadastrar cliente",
        description = "Associa o papel de cliente a uma pessoa ativa já cadastrada."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cliente cadastrado com sucesso"),
        @ApiResponse(
            responseCode = "400",
            description = "Pessoa inativa ou dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pessoa não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Pessoa já possui cadastro de cliente",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping
    public ResponseEntity<ClienteResponse> cadastrar(
        @Valid @RequestBody ClienteRequest request) {

        Cliente cliente = service.cadastrar(
            request.pessoaId(),
            request.telefone()
        );

        URI location = URI.create(
            "/api/clientes/" + cliente.getId()
        );

        return ResponseEntity.created(location)
            .body(mapper.toResponse(cliente));
    }

    @Operation(
        summary = "Pesquisar clientes",
        description = "Lista clientes com paginação, ordenação e filtros opcionais."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de clientes retornada"),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping
    public PageResponse<ClienteResponse> pesquisar(
        @Parameter(description = "Número da página, começando em zero", example = "0")
        @RequestParam(defaultValue = "0") int pagina,

        @Parameter(description = "Quantidade de itens por página, de 1 a 100", example = "10")
        @RequestParam(defaultValue = "10") int tamanho,

        @Parameter(description = "Campo usado para ordenação", example = "id")
        @RequestParam(defaultValue = "id") String ordenarPor,

        @Parameter(description = "Direção da ordenação: asc ou desc", example = "asc")
        @RequestParam(defaultValue = "asc") String direcao,

        @Parameter(description = "Filtra pelo nome da pessoa", example = "Bruno")
        @RequestParam(required = false) String nome,

        @Parameter(description = "Filtra pelo CPF da pessoa", example = "52998224725")
        @RequestParam(required = false) String cpf,

        @Parameter(description = "Filtra pela situação da pessoa", example = "ATIVO")
        @RequestParam(required = false) Status status
    ) {
        PageResponse<Cliente> resultado = service.pesquisar(
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
        summary = "Buscar cliente por ID",
        description = "Retorna um cliente e os dados da pessoa associada."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(
            responseCode = "404",
            description = "Cliente não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping("/{id}")
    public ClienteResponse buscarPorId(
        @Parameter(description = "Identificador do cliente", example = "1")
        @PathVariable Long id) {

        return mapper.toResponse(service.buscarPorId(id));
    }

    @Operation(
        summary = "Alterar cliente",
        description = "Altera o telefone do cliente. Nome, e-mail e CPF são alterados em /api/pessoas/{id}."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente atualizado"),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cliente não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PutMapping("/{id}")
    public ClienteResponse alterar(
        @Parameter(description = "Identificador do cliente", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody ClienteAtualizacaoRequest request) {

        return mapper.toResponse(
            service.alterar(id, request.telefone()));
    }
}
