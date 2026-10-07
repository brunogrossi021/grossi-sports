
package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.GrupoProdutoRequest;
import com.curso.grossisports.api.dto.GrupoProdutoResponse;
import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.api.mapper.GrupoProdutoMapper;
import com.curso.grossisports.domain.GrupoProduto;
import com.curso.grossisports.service.GrupoProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/grupos-produtos")
public class GrupoProdutoController {

    private final GrupoProdutoService service;
    private final GrupoProdutoMapper mapper;

    public GrupoProdutoController(
        GrupoProdutoService service,
        GrupoProdutoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

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

    @GetMapping("/{id}")
    public GrupoProdutoResponse buscarPorId(
        @PathVariable Long id) {

        return mapper.toResponse(
            service.buscarPorId(id));
    }

    @GetMapping
    public PageResponse<GrupoProdutoResponse> listar(
        @RequestParam(defaultValue = "0") int pagina,
        @RequestParam(defaultValue = "10") int tamanho,
        @RequestParam(defaultValue = "nome") String ordenarPor,
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
