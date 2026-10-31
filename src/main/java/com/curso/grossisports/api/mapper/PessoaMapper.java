package com.curso.grossisports.api.mapper;

import com.curso.grossisports.api.dto.PessoaResponse;
import com.curso.grossisports.domain.Pessoa;
import org.springframework.stereotype.Component;

@Component
public class PessoaMapper {

    public PessoaResponse toResponse(Pessoa pessoa) {
        return new PessoaResponse(
            pessoa.getId(),
            pessoa.getNome(),
            pessoa.getEmail(),
            pessoa.getCpf(),
            pessoa.getDataCadastro(),
            pessoa.getStatus()
        );
    }
}

