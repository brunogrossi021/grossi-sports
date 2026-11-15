package com.curso.grossisports.service;

import com.curso.grossisports.domain.EventoSeguranca;
import com.curso.grossisports.domain.TipoEventoSeguranca;
import com.curso.grossisports.repository.EventoSegurancaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Trilha de auditoria de segurança (Aula 17).
 *
 * <p>{@link #registrar} participa da transação corrente
 * ({@code REQUIRED}): em uma operação administrativa bem-sucedida, o
 * evento e a mudança de negócio confirmam ou revertem juntos. Se a
 * gravação obrigatória do evento falhar, a ação sensível também não é
 * confirmada — a implementação falha fechada.</p>
 *
 * <p>Nas falhas de autenticação a fronteira é outra: o serviço de
 * proteção chama {@code registrar} dentro de uma transação própria que
 * confirma antes de a exceção genérica ser lançada, preservando a
 * evidência mesmo com resposta HTTP de erro.</p>
 */
@Service
public class AuditoriaSegurancaService {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final EventoSegurancaRepository repository;
    private final Clock clock;

    public AuditoriaSegurancaService(
        EventoSegurancaRepository repository,
        Clock clock) {

        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Grava um evento de segurança. O instante é lido do {@link Clock}
     * controlável, permitindo testes temporais determinísticos.
     */
    @Transactional
    public EventoSeguranca registrar(
        TipoEventoSeguranca tipo,
        Long atorContaId,
        Long alvoContaId,
        String identificadorHash,
        String enderecoOrigem) {

        return repository.save(EventoSeguranca.registrar(
            tipo,
            clock.instant(),
            atorContaId,
            alvoContaId,
            identificadorHash,
            enderecoOrigem));
    }

    /**
     * Consulta paginada e cronológica da trilha. A autorização é
     * reafirmada aqui (defesa em profundidade), além da regra HTTP de
     * ADMINISTRADOR. A ordenação é decrescente por instante e, como
     * desempate, por identificador — coerente com a leitura de um
     * incidente.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<EventoSeguranca> consultar(
        TipoEventoSeguranca tipo, int pagina, int tamanho) {

        validarParametros(pagina, tamanho);

        Pageable pageable = PageRequest.of(
            pagina,
            tamanho,
            Sort.by(
                Sort.Order.desc("ocorridoEm"),
                Sort.Order.desc("id")));

        if (tipo == null) {
            return repository.findAll(pageable);
        }

        return repository.findByTipo(tipo, pageable);
    }

    private void validarParametros(int pagina, int tamanho) {
        if (pagina < 0) {
            throw new IllegalArgumentException(
                "A página não pode ser negativa");
        }

        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException(
                "O tamanho da página deve estar entre 1 e 100");
        }
    }
}
