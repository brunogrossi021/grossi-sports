package com.curso.grossisports.repository;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.PerfilAcesso;
import com.curso.grossisports.domain.StatusContaAcesso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContaAcessoRepository
    extends JpaRepository<ContaAcesso, Long> {

    boolean existsByLoginIgnoreCase(String login);

    boolean existsByPessoaId(Long pessoaId);

    @EntityGraph(attributePaths = {"pessoa", "perfis"})
    Optional<ContaAcesso> findByLoginIgnoreCase(String login);

    @EntityGraph(attributePaths = {"pessoa", "perfis"})
    Optional<ContaAcesso> findById(Long id);

    @Query("""
        select count(c)
        from ContaAcesso c
        join c.perfis p
        where p = :perfil
          and c.status = :status
        """)
    long contarPorPerfilEStatus(
        @Param("perfil") PerfilAcesso perfil,
        @Param("status") StatusContaAcesso status);

    /**
     * Bloqueia (pessimistic write) as contas que possuem o perfil
     * informado, ordenadas por id ascendente para manter uma ordem
     * de bloqueio consistente e reduzir risco de deadlock. Usado
     * antes de contar/alterar administradores (Aula 14).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select c
        from ContaAcesso c
        join c.perfis p
        where p = :perfil
        order by c.id asc
        """)
    List<ContaAcesso> travarPorPerfil(
        @Param("perfil") PerfilAcesso perfil);
}
