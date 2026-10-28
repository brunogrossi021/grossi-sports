
package com.curso.grossisports.repository;

import com.curso.grossisports.domain.Produto;
import com.curso.grossisports.domain.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends
    JpaRepository<Produto, Long>,
    JpaSpecificationExecutor<Produto> {

    Optional<Produto> findByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarras(String codigoBarras);

    List<Produto> findByGrupoId(Long grupoId);

    boolean existsByGrupoId(Long grupoId);

    List<Produto> findByStatus(Status status);

    @Override
    @EntityGraph(attributePaths = {"grupo", "fornecedor"})
    Optional<Produto> findById(Long id);


    @Override
    @EntityGraph(attributePaths = {"grupo", "fornecedor"})
    List<Produto> findAll();

    @EntityGraph(attributePaths = {"grupo", "fornecedor"})
    Page<Produto> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"grupo", "fornecedor"})
    Page<Produto> findAll(
        org.springframework.data.jpa.domain.Specification<Produto> spec,
        Pageable pageable);
}
