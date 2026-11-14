package com.curso.grossisports.service;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.ContaAcessoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class ContaAcessoService {

    private static final int TAMANHO_MINIMO_SENHA = 12;
    private static final int LIMITE_BCRYPT_BYTES = 72;

    private final ContaAcessoRepository repository;
    private final PessoaService pessoaService;
    private final PasswordEncoder passwordEncoder;

    public ContaAcessoService(
        ContaAcessoRepository repository,
        PessoaService pessoaService,
        PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.pessoaService = pessoaService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ContaAcesso cadastrar(
        String nome,
        String email,
        String cpf,
        String login,
        String senhaAberta) {

        validarSenha(senhaAberta);

        if (repository.existsByLoginIgnoreCase(login)) {
            throw new RecursoDuplicadoException(
                "Login já cadastrado");
        }

        Pessoa pessoa = pessoaService.cadastrar(nome, email, cpf);

        String senhaHash = passwordEncoder.encode(senhaAberta);

        return repository.save(
            new ContaAcesso(pessoa, login, senhaHash));
    }

    @Transactional(readOnly = true)
    public ContaAcesso buscarPorLogin(String login) {
        return repository.findByLoginIgnoreCase(login)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Conta de acesso não encontrada"));
    }

    @Transactional
    public void alterarSenha(
        String login, String senhaAtual, String novaSenha) {

        validarSenha(novaSenha);

        ContaAcesso conta = buscarPorLogin(login);

        if (!passwordEncoder.matches(
            senhaAtual, conta.getSenhaHash())) {
            throw new IllegalArgumentException(
                "Senha atual inválida");
        }

        if (passwordEncoder.matches(
            novaSenha, conta.getSenhaHash())) {
            throw new IllegalArgumentException(
                "Nova senha deve ser diferente da senha atual");
        }

        conta.alterarSenha(passwordEncoder.encode(novaSenha));
    }

    private void validarSenha(String senha) {
        if (senha == null
            || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException(
                "Senha deve possuir ao menos 12 caracteres");
        }

        if (senha.getBytes(StandardCharsets.UTF_8).length
            > LIMITE_BCRYPT_BYTES) {
            throw new IllegalArgumentException(
                "Senha deve possuir no máximo 72 bytes em UTF-8");
        }
    }
}
