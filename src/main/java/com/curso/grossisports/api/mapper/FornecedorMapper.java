package com.curso.grossisports.api.mapper;

import com.curso.grossisports.api.dto.FornecedorRequest;
import com.curso.grossisports.api.dto.FornecedorResponse;
import com.curso.grossisports.domain.Fornecedor;
import org.springframework.stereotype.Component;

@Component
public class FornecedorMapper {

    public Fornecedor toEntity(FornecedorRequest request) {
        return new Fornecedor(
            request.razaoSocial(),
            request.cnpj()
        );
    }

    public FornecedorResponse toResponse(Fornecedor fornecedor) {
        return new FornecedorResponse(
            fornecedor.getId(),
            fornecedor.getRazaoSocial(),
            fornecedor.getCnpj(),
            fornecedor.getStatus()
        );
    }
}
