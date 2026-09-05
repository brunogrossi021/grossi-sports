package com.curso.grossisports.repository;

import com.curso.grossisports.domain.GrupoProduto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GrupoProdutoRepository
    extends JpaRepository<GrupoProduto, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    Optional<GrupoProduto> findByNomeIgnoreCase(String nome);
}
