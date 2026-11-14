package com.curso.grossisports.api.mapper;

import com.curso.grossisports.api.dto.ContaAcessoResponse;
import com.curso.grossisports.domain.ContaAcesso;
import org.springframework.stereotype.Component;

@Component
public class ContaAcessoMapper {

    public ContaAcessoResponse toResponse(ContaAcesso conta) {
        return new ContaAcessoResponse(
            conta.getId(),
            conta.getPessoa().getId(),
            conta.getPessoa().getNome(),
            conta.getLogin(),
            conta.getStatus(),
            conta.getPerfis()
        );
    }
}
