package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.PessoaRequest;
import com.curso.grossisports.api.dto.PessoaResponse;
import com.curso.grossisports.api.mapper.PessoaMapper;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pessoas")
@Tag(
    name = "Pessoas",
    description = "Gerenciamento de pessoas cadastradas no sistema"
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
        summary = "Listar pessoas",
        description = "Retorna todas as pessoas cadastradas."
    )
    @GetMapping
    public List<PessoaResponse> listar() {
        return service.listar()
            .stream()
            .map(mapper::toResponse)
            .toList();
    }

    @Operation(
        summary = "Buscar pessoa por ID",
        description = "Retorna uma pessoa pelo seu identificador."
    )
    @GetMapping("/{id}")
    public PessoaResponse buscarPorId(@PathVariable Long id) {
        return mapper.toResponse(service.buscarPorId(id));
    }

    @Operation(
        summary = "Alterar pessoa",
        description = "Atualiza o nome, o e-mail e o CPF."
    )
    @PutMapping("/{id}")
    public PessoaResponse alterar(
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
        summary = "Ativar pessoa",
        description = "Ativa uma pessoa cadastrada."
    )
    @PatchMapping("/{id}/ativar")
    public PessoaResponse ativar(@PathVariable Long id) {
        return mapper.toResponse(service.ativar(id));
    }

    @Operation(
        summary = "Inativar pessoa",
        description = "Inativa uma pessoa cadastrada."
    )
    @PatchMapping("/{id}/inativar")
    public PessoaResponse inativar(@PathVariable Long id) {
        return mapper.toResponse(service.inativar(id));
    }
}

