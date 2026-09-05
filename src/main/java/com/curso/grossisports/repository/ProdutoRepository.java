package com.curso.grossisports.repository;

import com.curso.grossisports.domain.Produto;
import com.curso.grossisports.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository
    extends JpaRepository<Produto, Long> {

    Optional<Produto> findByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarras(String codigoBarras);

    List<Produto> findByGrupoId(Long grupoId);

    List<Produto> findByStatus(Status status);
}
