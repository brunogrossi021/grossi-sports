package com.curso.grossisports.repository;

import com.curso.grossisports.domain.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepository
    extends JpaRepository<Fornecedor, Long> {

    boolean existsByCnpj(String cnpj);
}
