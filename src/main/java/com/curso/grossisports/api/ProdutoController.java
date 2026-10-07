
package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.api.dto.ProdutoRequest;
import com.curso.grossisports.api.dto.ProdutoResponse;
import com.curso.grossisports.api.mapper.ProdutoMapper;
import com.curso.grossisports.domain.Produto;
import com.curso.grossisports.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService service;
    private final ProdutoMapper mapper;

    public ProdutoController(
        ProdutoService service,
        ProdutoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> cadastrar(
        @Valid @RequestBody ProdutoRequest request) {

        Produto produto = mapper.toEntity(request);

        Produto cadastrado = service.cadastrar(
            produto,
            request.grupoId(),
            request.fornecedorId());

        URI location = URI.create(
            "/api/produtos/" + cadastrado.getId());

        return ResponseEntity
            .created(location)
            .body(mapper.toResponse(cadastrado));
    }

    @GetMapping("/{id}")
    public ProdutoResponse buscarPorId(
        @PathVariable Long id) {

        return mapper.toResponse(
            service.buscarPorId(id));
    }

    @GetMapping
    public PageResponse<ProdutoResponse> listar(
        @RequestParam(defaultValue = "0") int pagina,
        @RequestParam(defaultValue = "10") int tamanho,
        @RequestParam(defaultValue = "descricao") String ordenarPor,
        @RequestParam(defaultValue = "asc") String direcao) {

        PageResponse<Produto> resultado =
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
