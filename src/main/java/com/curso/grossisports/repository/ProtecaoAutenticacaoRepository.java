package com.curso.grossisports.repository;

import com.curso.grossisports.domain.ProtecaoAutenticacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Acesso ao contador de falhas de login (Aula 17).
 *
 * <p>Duas falhas simultâneas não podem perder atualização. Por isso a
 * leitura para escrita usa {@code SELECT ... FOR UPDATE}
 * ({@link LockModeType#PESSIMISTIC_WRITE}) e a primeira linha é criada
 * com um <em>upsert</em> nativo do PostgreSQL
 * ({@code INSERT ... ON CONFLICT DO NOTHING}), que tolera a corrida de
 * duas transações tentando inserir a mesma chave.</p>
 */
public interface ProtecaoAutenticacaoRepository
    extends JpaRepository<ProtecaoAutenticacao, Long> {

    Optional<ProtecaoAutenticacao> findByChaveLogin(String chaveLogin);

    /**
     * Cria a linha do contador se ela ainda não existir, sem falhar
     * quando outra transação a criou simultaneamente. A cláusula
     * {@code ON CONFLICT} é específica do PostgreSQL: não há
     * portabilidade fingida para outros SGBDs.
     */
    @Modifying(flushAutomatically = true)
    @Query(
        value = """
            insert into protecao_autenticacao
                (chave_login, falhas_consecutivas)
            values (:chaveLogin, 0)
            on conflict (chave_login) do nothing
            """,
        nativeQuery = true)
    void inserirSeAusente(@Param("chaveLogin") String chaveLogin);

    /**
     * Localiza o contador aplicando bloqueio pessimista. A linha
     * permanece bloqueada até o fim da transação, de modo que a
     * segunda falha concorrente só lê o valor já atualizado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select protecao
        from ProtecaoAutenticacao protecao
        where protecao.chaveLogin = :chaveLogin
        """)
    Optional<ProtecaoAutenticacao> buscarParaAtualizacao(
        @Param("chaveLogin") String chaveLogin);
}
