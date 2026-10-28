package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.FornecedorRequest;
import com.curso.grossisports.api.dto.FornecedorResponse;
import com.curso.grossisports.api.mapper.FornecedorMapper;
import com.curso.grossisports.domain.Fornecedor;
import com.curso.grossisports.service.FornecedorService;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.List;

@RestController
@RequestMapping("/api/fornecedores")
@Tag(
    name = "Fornecedores",
    description = "Gerenciamento de fornecedores de produtos esportivos"
)
public class FornecedorController {

    private final FornecedorService service;
    private final FornecedorMapper mapper;

    public FornecedorController(
        FornecedorService service,
        FornecedorMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Cadastrar fornecedor",
        description = "Cadastra um fornecedor com razão social e CNPJ."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Fornecedor cadastrado com sucesso"
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
                          "path": "/api/fornecedores",
                          "fields": {
                            "cnpj": "CNPJ inválido"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "CNPJ já cadastrado",
            content = @Content(
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "CNPJ duplicado",
                    value = """
                        {
                          "timestamp": "2026-10-22T18:30:00Z",
                          "status": 409,
                          "error": "Conflict",
                          "message": "CNPJ já cadastrado",
                          "path": "/api/fornecedores",
                          "fields": {}
                        }
                        """
                )
            )
        )
    })
    @PostMapping
    public ResponseEntity<FornecedorResponse> cadastrar(
        @Valid @RequestBody FornecedorRequest request) {

        Fornecedor fornecedor = mapper.toEntity(request);

        Fornecedor cadastrado = service.cadastrar(
            fornecedor.getRazaoSocial(),
            fornecedor.getCnpj());

        URI location = URI.create(
            "/api/fornecedores/" + cadastrado.getId());

        return ResponseEntity.created(location)
            .body(mapper.toResponse(cadastrado));
    }

    @Operation(
        summary = "Listar fornecedores",
        description = "Retorna a lista de fornecedores cadastrados."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de fornecedores retornada"
        )
    })
    @GetMapping
    public List<FornecedorResponse> listar() {
        return service.listar()
            .stream()
            .map(mapper::toResponse)
            .toList();
    }
}
