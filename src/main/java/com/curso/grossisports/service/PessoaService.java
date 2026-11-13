package com.curso.grossisports.service;

import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.Status;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.PessoaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

@Service
public class PessoaService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
        "id",
        "nome",
        "email",
        "cpf",
        "dataCadastro",
        "status"
    );

    private final PessoaRepository repository;

    public PessoaService(PessoaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Pessoa cadastrar(String nome, String email, String cpf) {
        validarUnicidade(cpf, email, null);

        Pessoa pessoa = new Pessoa(
            nome,
            email,
            cpf,
            LocalDate.now()
        );

        return repository.save(pessoa);
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

        validarUnicidade(cpf, email, id);

        pessoa.alterarDados(nome, email, cpf);

        return repository.save(pessoa);
    }

    @Transactional
    public Pessoa alterarStatus(Long id, Status status) {
        if (status == null) {
            throw new IllegalArgumentException(
                "Status é obrigatório");
        }

        Pessoa pessoa = buscarPorId(id);

        if (status == Status.ATIVO) {
            pessoa.ativar();
        } else {
            pessoa.inativar();
        }

        return repository.save(pessoa);
    }

    @Transactional
    public Pessoa ativar(Long id) {
        return alterarStatus(id, Status.ATIVO);
    }

    @Transactional
    public Pessoa inativar(Long id) {
        return alterarStatus(id, Status.INATIVO);
    }

    @Transactional(readOnly = true)
    public PageResponse<Pessoa> pesquisar(
        String nome,
        String cpf,
        Status status,
        int pagina,
        int tamanho,
        String ordenarPor,
        String direcao
    ) {
        validarParametrosPaginacao(
            pagina, tamanho, ordenarPor, direcao);

        Sort sort = "desc".equalsIgnoreCase(direcao)
            ? Sort.by(ordenarPor).descending()
            : Sort.by(ordenarPor).ascending();

        Pageable pageable = PageRequest.of(pagina, tamanho, sort);

        Specification<Pessoa> filtros =
            (root, query, cb) -> cb.conjunction();

        if (nome != null && !nome.isBlank()) {
            String trecho =
                "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";

            filtros = filtros.and((root, query, cb) ->
                cb.like(cb.lower(root.get("nome")), trecho));
        }

        if (cpf != null && !cpf.isBlank()) {
            String cpfNormalizado = cpf.trim();

            filtros = filtros.and((root, query, cb) ->
                cb.equal(root.get("cpf"), cpfNormalizado));
        }

        if (status != null) {
            filtros = filtros.and((root, query, cb) ->
                cb.equal(root.get("status"), status));
        }

        Page<Pessoa> resultado = repository.findAll(filtros, pageable);

        return new PageResponse<>(
            resultado.getContent(),
            resultado.getNumber(),
            resultado.getSize(),
            resultado.getTotalElements(),
            resultado.getTotalPages()
        );
    }

    private void validarUnicidade(
        String cpf,
        String email,
        Long idAtual
    ) {
        String cpfNormalizado =
            cpf == null ? null : cpf.trim();

        String emailNormalizado = email == null
            ? null
            : email.trim().toLowerCase(Locale.ROOT);

        boolean cpfDuplicado = cpfNormalizado != null
            && (idAtual == null
                ? repository.existsByCpf(cpfNormalizado)
                : repository.existsByCpfAndIdNot(
                    cpfNormalizado, idAtual));

        boolean emailDuplicado = emailNormalizado != null
            && (idAtual == null
                ? repository.existsByEmailIgnoreCase(
                    emailNormalizado)
                : repository.existsByEmailIgnoreCaseAndIdNot(
                    emailNormalizado, idAtual));

        if (cpfDuplicado) {
            throw new RecursoDuplicadoException(
                "CPF já cadastrado");
        }

        if (emailDuplicado) {
            throw new RecursoDuplicadoException(
                "E-mail já cadastrado");
        }
    }

    private void validarParametrosPaginacao(
        int pagina,
        int tamanho,
        String ordenarPor,
        String direcao
    ) {
        if (pagina < 0) {
            throw new IllegalArgumentException(
                "A página não pode ser negativa");
        }

        if (tamanho < 1 || tamanho > 100) {
            throw new IllegalArgumentException(
                "O tamanho da página deve estar entre 1 e 100");
        }

        if (ordenarPor == null
            || !CAMPOS_ORDENACAO.contains(ordenarPor)) {
            throw new IllegalArgumentException(
                "Campo de ordenação inválido");
        }

        if (direcao == null
            || (!"asc".equalsIgnoreCase(direcao)
            && !"desc".equalsIgnoreCase(direcao))) {
            throw new IllegalArgumentException(
                "A direção deve ser asc ou desc");
        }
    }
}
