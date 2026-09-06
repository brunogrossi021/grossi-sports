package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.FornecedorRequest;
import com.curso.grossisports.api.dto.FornecedorResponse;
import com.curso.grossisports.api.mapper.FornecedorMapper;
import com.curso.grossisports.domain.Fornecedor;
import com.curso.grossisports.service.FornecedorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {

    private final FornecedorService service;
    private final FornecedorMapper mapper;

    public FornecedorController(
        FornecedorService service,
        FornecedorMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<FornecedorResponse> cadastrar(
        @Valid @RequestBody FornecedorRequest request) {

        Fornecedor fornecedor = mapper.toEntity(request);

        Fornecedor cadastrado = service.cadastrar(
            fornecedor.getRazaoSocial(),
            fornecedor.getCnpj());

        URI location = URI.create(
            "/api/fornecedores/" + cadastrado.getId());

        return ResponseEntity
            .created(location)
            .body(mapper.toResponse(cadastrado));
    }
}
