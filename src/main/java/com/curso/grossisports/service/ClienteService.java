package com.curso.grossisports.service;

import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.domain.Cliente;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.Status;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.ClienteRepository;
import com.curso.grossisports.repository.PessoaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class ClienteService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
        "id",
        "telefone",
        "pessoa.nome",
        "pessoa.cpf",
        "pessoa.status"
    );

    private final ClienteRepository clienteRepository;
    private final PessoaRepository pessoaRepository;

    public ClienteService(
        ClienteRepository clienteRepository,
        PessoaRepository pessoaRepository
    ) {
        this.clienteRepository = clienteRepository;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional
    public Cliente cadastrar(Long pessoaId, String telefone) {
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Pessoa não encontrada"));

        if (pessoa.getStatus() != Status.ATIVO) {
            throw new IllegalArgumentException(
                "Pessoa deve estar ativa");
        }

        if (clienteRepository.existsByPessoaId(pessoaId)) {
            throw new RecursoDuplicadoException(
                "Pessoa já possui cadastro de cliente");
        }

        Cliente cliente = new Cliente(pessoa, telefone);

        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findOneById(id)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Cliente não encontrado"));
    }

    @Transactional
    public Cliente alterar(Long id, String telefone) {
        Cliente cliente = buscarPorId(id);
        cliente.alterarTelefone(telefone);
        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public PageResponse<Cliente> pesquisar(
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

        Specification<Cliente> filtros =
            (root, query, cb) -> cb.conjunction();

        if (nome != null && !nome.isBlank()) {
            String trecho =
                "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";

            filtros = filtros.and((root, query, cb) ->
                cb.like(
                    cb.lower(root.get("pessoa").get("nome")),
                    trecho));
        }

        if (cpf != null && !cpf.isBlank()) {
            String cpfNormalizado = cpf.trim();

            filtros = filtros.and((root, query, cb) ->
                cb.equal(
                    root.get("pessoa").get("cpf"),
                    cpfNormalizado));
        }

        if (status != null) {
            filtros = filtros.and((root, query, cb) ->
                cb.equal(
                    root.get("pessoa").get("status"),
                    status));
        }

        Page<Cliente> resultado =
            clienteRepository.findAll(filtros, pageable);

        return new PageResponse<>(
            resultado.getContent(),
            resultado.getNumber(),
            resultado.getSize(),
            resultado.getTotalElements(),
            resultado.getTotalPages()
        );
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
