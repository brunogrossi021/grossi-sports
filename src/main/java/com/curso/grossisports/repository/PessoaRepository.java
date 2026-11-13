package com.curso.grossisports.repository;

import com.curso.grossisports.domain.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PessoaRepository
    extends JpaRepository<Pessoa, Long>,
    JpaSpecificationExecutor<Pessoa> {

    Optional<Pessoa> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
