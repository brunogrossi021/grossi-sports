package com.curso.grossisports.service;

import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PessoaService {

    private final PessoaRepository repository;

    public PessoaService(PessoaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Pessoa cadastrar(String nome, String email, String cpf) {
        String cpfNormalizado = cpf == null ? null : cpf.trim();

        if (cpfNormalizado != null
                && repository.existsByCpf(cpfNormalizado)) {
            throw new RecursoDuplicadoException("CPF já cadastrado");
        }

        Pessoa pessoa = new Pessoa(
            nome,
            email,
            cpf,
            LocalDate.now()
        );

        return repository.save(pessoa);
    }

    @Transactional(readOnly = true)
    public List<Pessoa> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Pessoa buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Pessoa não encontrada"));
    }

    @Transactional
    public Pessoa alterar(
            Long id, String nome, String email, String cpf) {
        Pessoa pessoa = buscarPorId(id);

        String cpfNormalizado = cpf == null ? null : cpf.trim();

        if (cpfNormalizado != null
                && !cpfNormalizado.equals(pessoa.getCpf())
                && repository.existsByCpf(cpfNormalizado)) {
            throw new RecursoDuplicadoException("CPF já cadastrado");
        }

        pessoa.alterarDados(nome, email, cpf);

        return repository.save(pessoa);
    }

    @Transactional
    public Pessoa ativar(Long id) {
        Pessoa pessoa = buscarPorId(id);
        pessoa.ativar();
        return repository.save(pessoa);
    }

    @Transactional
    public Pessoa inativar(Long id) {
        Pessoa pessoa = buscarPorId(id);
        pessoa.inativar();
        return repository.save(pessoa);
    }
}
