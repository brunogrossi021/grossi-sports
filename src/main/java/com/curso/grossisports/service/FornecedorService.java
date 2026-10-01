package com.curso.grossisports.service;

import com.curso.grossisports.domain.Fornecedor;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.FornecedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FornecedorService {

    private final FornecedorRepository repository;

    public FornecedorService(FornecedorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Fornecedor cadastrar(
        String razaoSocial,
        String cnpj) {

        if (repository.existsByCnpj(cnpj)) {
            throw new RecursoDuplicadoException(
                "CNPJ já cadastrado");
        }

        return repository.save(
            new Fornecedor(razaoSocial, cnpj));
    }

    @Transactional(readOnly = true)
    public Fornecedor buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Fornecedor não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> listar() {
        return repository.findAll();
    }
}
