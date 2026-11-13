package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.ColaboradorAtualizacaoRequest;
import com.curso.grossisports.api.dto.ColaboradorRequest;
import com.curso.grossisports.api.dto.ColaboradorResponse;
import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.api.mapper.ColaboradorMapper;
import com.curso.grossisports.domain.Colaborador;
import com.curso.grossisports.domain.FuncaoColaborador;
import com.curso.grossisports.domain.Status;
import com.curso.grossisports.service.ColaboradorService;
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
@RequestMapping("/api/colaboradores")
@Tag(
    name = "Colaboradores",
    description = "Vínculo profissional e função de negócio"
)
public class ColaboradorController {

    private final ColaboradorService service;
    private final ColaboradorMapper mapper;

    public ColaboradorController(
        ColaboradorService service,
        ColaboradorMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Cadastrar colaborador",
        description = "Associa um vínculo profissional a uma pessoa ativa."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Colaborador cadastrado com sucesso"),
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
            description = "Pessoa já cadastrada como colaborador ou matrícula duplicada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping
    public ResponseEntity<ColaboradorResponse> cadastrar(
        @Valid @RequestBody ColaboradorRequest request) {

        Colaborador colaborador = service.cadastrar(
            request.pessoaId(),
            request.matricula(),
            request.funcao(),
            request.dataAdmissao()
        );

        URI location = URI.create(
            "/api/colaboradores/" + colaborador.getId()
        );

        return ResponseEntity.created(location)
            .body(mapper.toResponse(colaborador));
    }

    @Operation(
        summary = "Pesquisar colaboradores",
        description = "Lista colaboradores com paginação, ordenação e filtros opcionais."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de colaboradores retornada"),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping
    public PageResponse<ColaboradorResponse> pesquisar(
        @Parameter(description = "Número da página, começando em zero", example = "0")
        @RequestParam(defaultValue = "0") int pagina,

        @Parameter(description = "Quantidade de itens por página, de 1 a 100", example = "10")
        @RequestParam(defaultValue = "10") int tamanho,

        @Parameter(description = "Campo usado para ordenação", example = "id")
        @RequestParam(defaultValue = "id") String ordenarPor,

        @Parameter(description = "Direção da ordenação: asc ou desc", example = "asc")
        @RequestParam(defaultValue = "asc") String direcao,

        @Parameter(description = "Filtra pelo nome da pessoa", example = "Carlos")
        @RequestParam(required = false) String nome,

        @Parameter(description = "Filtra pela função do colaborador", example = "VENDEDOR")
        @RequestParam(required = false) FuncaoColaborador funcao,

        @Parameter(description = "Filtra pela situação da pessoa", example = "ATIVO")
        @RequestParam(required = false) Status status
    ) {
        PageResponse<Colaborador> resultado = service.pesquisar(
            nome,
            funcao,
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
        summary = "Buscar colaborador por ID",
        description = "Retorna um colaborador e os dados da pessoa associada."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Colaborador encontrado"),
        @ApiResponse(
            responseCode = "404",
            description = "Colaborador não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping("/{id}")
    public ColaboradorResponse buscarPorId(
        @Parameter(description = "Identificador do colaborador", example = "1")
        @PathVariable Long id) {

        return mapper.toResponse(service.buscarPorId(id));
    }

    @Operation(
        summary = "Alterar colaborador",
        description = "Altera matrícula e função de negócio. Não concede permissões de acesso."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Colaborador atualizado"),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Colaborador não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Matrícula já cadastrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PutMapping("/{id}")
    public ColaboradorResponse alterar(
        @Parameter(description = "Identificador do colaborador", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody ColaboradorAtualizacaoRequest request) {

        return mapper.toResponse(
            service.alterar(
                id,
                request.matricula(),
                request.funcao())
        );
    }
}
