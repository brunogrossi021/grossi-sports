package com.curso.grossisports.repository;

import com.curso.grossisports.domain.EventoSeguranca;
import com.curso.grossisports.domain.TipoEventoSeguranca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acesso à trilha de auditoria de segurança (Aula 17).
 *
 * <p>A aplicação escreve e o administrador lê. Não existe operação de
 * criação, alteração ou exclusão exposta pela API.</p>
 */
public interface EventoSegurancaRepository
    extends JpaRepository<EventoSeguranca, Long> {

    Page<EventoSeguranca> findByTipo(
        TipoEventoSeguranca tipo, Pageable pageable);

    long countByTipo(TipoEventoSeguranca tipo);

    long countByTipoAndIdentificadorHash(
        TipoEventoSeguranca tipo, String identificadorHash);

    long countByTipoAndAlvoContaId(
        TipoEventoSeguranca tipo, Long alvoContaId);

    long countByTipoAndAtorContaId(
        TipoEventoSeguranca tipo, Long atorContaId);

    List<EventoSeguranca> findByIdentificadorHashOrderByIdAsc(
        String identificadorHash);
}
