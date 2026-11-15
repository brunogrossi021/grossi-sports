package com.curso.grossisports.api.mapper;

import com.curso.grossisports.api.dto.EventoSegurancaResponse;
import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.domain.EventoSeguranca;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Converte eventos de segurança para o DTO de consulta (Aula 17).
 * O hash de correlação é deliberadamente omitido do DTO.
 */
@Component
public class EventoSegurancaMapper {

    public EventoSegurancaResponse toResponse(EventoSeguranca evento) {
        return new EventoSegurancaResponse(
            evento.getId(),
            evento.getTipo(),
            evento.getOcorridoEm(),
            evento.getAtorContaId(),
            evento.getAlvoContaId(),
            evento.getEnderecoOrigem());
    }

    public PageResponse<EventoSegurancaResponse> toPage(
        Page<EventoSeguranca> pagina) {

        return new PageResponse<>(
            pagina.getContent().stream()
                .map(this::toResponse)
                .toList(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages());
    }
}
