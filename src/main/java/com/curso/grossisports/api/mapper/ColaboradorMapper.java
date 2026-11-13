package com.curso.grossisports.api.mapper;

import com.curso.grossisports.api.dto.ColaboradorResponse;
import com.curso.grossisports.domain.Colaborador;
import com.curso.grossisports.domain.Pessoa;
import org.springframework.stereotype.Component;

@Component
public class ColaboradorMapper {

    public ColaboradorResponse toResponse(Colaborador colaborador) {
        Pessoa pessoa = colaborador.getPessoa();

        return new ColaboradorResponse(
            colaborador.getId(),
            pessoa.getId(),
            pessoa.getNome(),
            pessoa.getEmail(),
            pessoa.getCpf(),
            pessoa.getStatus(),
            colaborador.getMatricula(),
            colaborador.getFuncao(),
            colaborador.getDataAdmissao()
        );
    }
}
