package com.curso.grossisports.api.mapper;

import com.curso.grossisports.api.dto.ClienteResponse;
import com.curso.grossisports.domain.Cliente;
import com.curso.grossisports.domain.Pessoa;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public ClienteResponse toResponse(Cliente cliente) {
        Pessoa pessoa = cliente.getPessoa();

        return new ClienteResponse(
            cliente.getId(),
            pessoa.getId(),
            pessoa.getNome(),
            pessoa.getEmail(),
            pessoa.getCpf(),
            pessoa.getStatus(),
            cliente.getTelefone()
        );
    }
}
