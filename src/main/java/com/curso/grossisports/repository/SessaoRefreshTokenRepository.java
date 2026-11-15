package com.curso.grossisports.repository;

import com.curso.grossisports.domain.MotivoRevogacaoRefreshToken;
import com.curso.grossisports.domain.SessaoRefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Acesso às sessões de refresh token (Aula 16).
 *
 * <p>A busca por hash usa bloqueio pessimista para que duas renovações
 * concorrentes do mesmo token não gerem dois sucessores: a primeira
 * transação bloqueia a linha, marca o token como utilizado e confirma;
 * a segunda encontra o token já utilizado e revoga a família.</p>
 */
public interface SessaoRefreshTokenRepository
    extends JpaRepository<SessaoRefreshToken, Long> {

    /**
     * Localiza a geração pelo hash do token aplicando
     * {@code SELECT ... FOR UPDATE}.
     *
     * <p>Os {@code join fetch} são internos (a conta e a pessoa são
     * obrigatórias pela modelagem), portanto o {@code FOR UPDATE} do
     * PostgreSQL incide sobre junções não anuláveis e não sofre a
     * restrição de bloqueio do lado opcional de um outer join. A conta
     * e a pessoa são trazidas na mesma consulta para a renovação poder
     * revalidar {@code podeAutenticar()} sem consultas adicionais.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select sessao
        from SessaoRefreshToken sessao
        join fetch sessao.conta conta
        join fetch conta.pessoa
        where sessao.tokenHash = :tokenHash
        """)
    Optional<SessaoRefreshToken> buscarPorHashParaAtualizacao(
        @Param("tokenHash") String tokenHash);

    /**
     * Lista as gerações de uma conta em ordem de criação (pelo id, que é
     * monotônico). Apoia a inspeção das sessões e a limpeza em testes.
     */
    List<SessaoRefreshToken> findByContaIdOrderByIdAsc(Long contaId);

    /**
     * Revoga todas as gerações ainda ativas de uma família, registrando
     * o motivo. Usado no logout local e na detecção de reutilização.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SessaoRefreshToken sessao
        set sessao.revogadaEm = :agora,
            sessao.motivoRevogacao = :motivo
        where sessao.familiaId = :familiaId
          and sessao.revogadaEm is null
        """)
    int revogarFamilia(
        @Param("familiaId") String familiaId,
        @Param("agora") Instant agora,
        @Param("motivo") MotivoRevogacaoRefreshToken motivo);

    /**
     * Revoga todas as gerações ainda ativas de uma conta, encerrando
     * todas as sessões (logout global).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SessaoRefreshToken sessao
        set sessao.revogadaEm = :agora,
            sessao.motivoRevogacao = :motivo
        where sessao.conta.id = :contaId
          and sessao.revogadaEm is null
        """)
    int revogarTodasDaConta(
        @Param("contaId") Long contaId,
        @Param("agora") Instant agora,
        @Param("motivo") MotivoRevogacaoRefreshToken motivo);
}
