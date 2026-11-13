package com.curso.grossisports.service;

import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.domain.Colaborador;
import com.curso.grossisports.domain.FuncaoColaborador;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.Status;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.ColaboradorRepository;
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
public class ColaboradorService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
        "id",
        "matricula",
        "funcao",
        "dataAdmissao",
        "pessoa.nome",
        "pessoa.status"
    );

    private final ColaboradorRepository colaboradorRepository;
    private final PessoaRepository pessoaRepository;

    public ColaboradorService(
        ColaboradorRepository colaboradorRepository,
        PessoaRepository pessoaRepository
    ) {
        this.colaboradorRepository = colaboradorRepository;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional
    public Colaborador cadastrar(
        Long pessoaId,
        String matricula,
        FuncaoColaborador funcao,
        LocalDate dataAdmissao
    ) {
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Pessoa não encontrada"));

        if (pessoa.getStatus() != Status.ATIVO) {
            throw new IllegalArgumentException(
                "Pessoa deve estar ativa");
        }

        if (colaboradorRepository.existsByPessoaId(pessoaId)) {
            throw new RecursoDuplicadoException(
                "Pessoa já possui cadastro de colaborador");
        }

        String matriculaNormalizada =
            matricula == null ? null : matricula.trim();

        if (matriculaNormalizada != null
            && colaboradorRepository.existsByMatricula(
            matriculaNormalizada)) {
            throw new RecursoDuplicadoException(
                "Matrícula já cadastrada");
        }

        Colaborador colaborador = new Colaborador(
            pessoa,
            matriculaNormalizada,
            funcao,
            dataAdmissao
        );

        return colaboradorRepository.save(colaborador);
    }

    @Transactional(readOnly = true)
    public Colaborador buscarPorId(Long id) {
        return colaboradorRepository.findOneById(id)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Colaborador não encontrado"));
    }

    @Transactional
    public Colaborador alterar(
        Long id,
        String matricula,
        FuncaoColaborador funcao
    ) {
        Colaborador colaborador = buscarPorId(id);

        String matriculaNormalizada =
            matricula == null ? null : matricula.trim();

        if (matriculaNormalizada != null
            && colaboradorRepository.existsByMatriculaAndIdNot(
            matriculaNormalizada, id)) {
            throw new RecursoDuplicadoException(
                "Matrícula já cadastrada");
        }

        colaborador.alterarDados(matriculaNormalizada, funcao);

        return colaboradorRepository.save(colaborador);
    }

    @Transactional(readOnly = true)
    public PageResponse<Colaborador> pesquisar(
        String nome,
        FuncaoColaborador funcao,
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

        Specification<Colaborador> filtros =
            (root, query, cb) -> cb.conjunction();

        if (nome != null && !nome.isBlank()) {
            String trecho =
                "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";

            filtros = filtros.and((root, query, cb) ->
                cb.like(
                    cb.lower(root.get("pessoa").get("nome")),
                    trecho));
        }

        if (funcao != null) {
            filtros = filtros.and((root, query, cb) ->
                cb.equal(root.get("funcao"), funcao));
        }

        if (status != null) {
            filtros = filtros.and((root, query, cb) ->
                cb.equal(
                    root.get("pessoa").get("status"),
                    status));
        }

        Page<Colaborador> resultado =
            colaboradorRepository.findAll(filtros, pageable);

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
