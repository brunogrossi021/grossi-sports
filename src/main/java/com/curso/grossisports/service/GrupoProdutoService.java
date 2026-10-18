package com.curso.grossisports.service;

import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.domain.GrupoProduto;
import com.curso.grossisports.exception.GrupoProdutoComProdutosException;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.GrupoProdutoRepository;
import com.curso.grossisports.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class GrupoProdutoService {

    private static final Set<String> CAMPOS_ORDENACAO =
        Set.of("id", "nome", "status");

    private final GrupoProdutoRepository repository;
    private final ProdutoRepository produtoRepository;

    public GrupoProdutoService(
        GrupoProdutoRepository repository,
        ProdutoRepository produtoRepository) {
        this.repository = repository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public GrupoProduto cadastrar(String nome) {
        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new RecursoDuplicadoException(
                "Nome do grupo já cadastrado");
        }

        return repository.save(new GrupoProduto(nome));
    }

    @Transactional
    public GrupoProduto editar(Long id, String nome) {
        GrupoProduto grupo = buscarPorId(id);

        repository.findByNomeIgnoreCase(nome)
            .filter(outro -> !outro.getId().equals(id))
            .ifPresent(outro -> {
                throw new RecursoDuplicadoException(
                    "Nome do grupo já cadastrado");
            });

        grupo.alterarNome(nome);

        return repository.save(grupo);
    }

    @Transactional
    public GrupoProduto ativar(Long id) {
        GrupoProduto grupo = buscarPorId(id);
        grupo.ativar();
        return repository.save(grupo);
    }

    @Transactional
    public GrupoProduto inativar(Long id) {
        GrupoProduto grupo = buscarPorId(id);
        grupo.inativar();
        return repository.save(grupo);
    }

    @Transactional
    public void excluir(Long id) {
        GrupoProduto grupo = buscarPorId(id);

        if (produtoRepository.existsByGrupoId(id)) {
            throw new GrupoProdutoComProdutosException(
                "Não é possível excluir um grupo que possui produtos vinculados");
        }

        repository.delete(grupo);
    }

    @Transactional(readOnly = true)
    public GrupoProduto buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Grupo de produto não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<GrupoProduto> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public PageResponse<GrupoProduto> listarPaginado(
        int pagina, int tamanho, String ordenarPor, String direcao) {

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
                "Campo de ordenação inválido. Use: id, nome ou status");
        }

        if (direcao == null
            || (!"asc".equalsIgnoreCase(direcao)
            && !"desc".equalsIgnoreCase(direcao))) {
            throw new IllegalArgumentException(
                "A direção deve ser asc ou desc");
        }

        Sort sort = "desc".equalsIgnoreCase(direcao)
            ? Sort.by(ordenarPor).descending()
            : Sort.by(ordenarPor).ascending();

        Pageable pageable = PageRequest.of(pagina, tamanho, sort);
        Page<GrupoProduto> resultado = repository.findAll(pageable);

        return new PageResponse<>(
            resultado.getContent(),
            resultado.getNumber(),
            resultado.getSize(),
            resultado.getTotalElements(),
            resultado.getTotalPages()
        );
    }
}
