package com.curso.grossisports.service;

import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.api.dto.ProdutoUpdateRequest;
import com.curso.grossisports.domain.Fornecedor;
import com.curso.grossisports.domain.GrupoProduto;
import com.curso.grossisports.domain.Produto;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.FornecedorRepository;
import com.curso.grossisports.repository.GrupoProdutoRepository;
import com.curso.grossisports.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.curso.grossisports.domain.Status;
import com.curso.grossisports.repository.ProdutoSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class ProdutoService {

    private static final Set<String> CAMPOS_ORDENACAO =
        Set.of(
            "id",
            "codigoBarras",
            "descricao",
            "saldoEstoque",
            "valorUnitario",
            "estoqueMinimo",
            "dataCadastro",
            "status"
        );

    private final ProdutoRepository produtoRepository;
    private final GrupoProdutoRepository grupoRepository;
    private final FornecedorRepository fornecedorRepository;

    public ProdutoService(
        ProdutoRepository produtoRepository,
        GrupoProdutoRepository grupoRepository,
        FornecedorRepository fornecedorRepository) {
        this.produtoRepository = produtoRepository;
        this.grupoRepository = grupoRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @Transactional
    public Produto cadastrar(
        Produto produto,
        Long grupoId,
        Long fornecedorId) {

        if (produtoRepository.existsByCodigoBarras(
            produto.getCodigoBarras())) {
            throw new RecursoDuplicadoException(
                "Código de barras já cadastrado");
        }

        GrupoProduto grupo = grupoRepository.findById(grupoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Grupo de produto não encontrado"));

        grupo.adicionarProduto(produto);

        if (fornecedorId != null) {
            Fornecedor fornecedor = fornecedorRepository.findById(
                    fornecedorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                    "Fornecedor não encontrado"));

            produto.associarFornecedor(fornecedor);
        }

        return produtoRepository.save(produto);
    }

    @Transactional(readOnly = true)
    public Produto buscarPorId(Long id) {
        return produtoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Produto não encontrado"));
    }

    @Transactional
    public Produto editar(Long id, ProdutoUpdateRequest request) {
        Produto produto = buscarPorId(id);

        produto.alterarDescricao(request.descricao());
        produto.alterarValorUnitario(request.valorUnitario());
        produto.alterarEstoqueMinimo(request.estoqueMinimo());

        GrupoProduto grupo = grupoRepository.findById(request.grupoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Grupo de produto não encontrado"));

        produto.alterarGrupo(grupo);

        if (request.fornecedorId() != null) {
            Fornecedor fornecedor = fornecedorRepository.findById(
                    request.fornecedorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                    "Fornecedor não encontrado"));

            produto.alterarFornecedor(fornecedor);
        } else {
            produto.alterarFornecedor(null);
        }

        return produtoRepository.save(produto);
    }

    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return produtoRepository.findAll();
    }


    @Transactional(readOnly = true)
    public PageResponse<Produto> listarPaginado(
        int pagina, int tamanho, String ordenarPor, String direcao) {

        return listarPaginado(
            pagina, tamanho, ordenarPor, direcao,
            null, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<Produto> listarPaginado(
        int pagina,
        int tamanho,
        String ordenarPor,
        String direcao,
        String descricao,
        String codigoBarras,
        Long grupoId,
        Status status,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo) {

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

        if (valorMinimo != null
            && valorMaximo != null
            && valorMinimo.compareTo(valorMaximo) > 0) {
            throw new IllegalArgumentException(
                "O valor mínimo não pode ser maior que o valor máximo");
        }

        Sort sort = "desc".equalsIgnoreCase(direcao)
            ? Sort.by(ordenarPor).descending()
            : Sort.by(ordenarPor).ascending();

        Pageable pageable = PageRequest.of(pagina, tamanho, sort);

        Specification<Produto> filtros =
            ProdutoSpecification.descricaoContem(descricao)
                .and(ProdutoSpecification.porCodigoBarras(codigoBarras))
                .and(ProdutoSpecification.porGrupo(grupoId))
                .and(ProdutoSpecification.porStatus(status))
                .and(ProdutoSpecification
                    .valorUnitarioMaiorOuIgual(valorMinimo))
                .and(ProdutoSpecification
                    .valorUnitarioMenorOuIgual(valorMaximo));

        Page<Produto> resultado =
            produtoRepository.findAll(filtros, pageable);

        return new PageResponse<>(
            resultado.getContent(),
            resultado.getNumber(),
            resultado.getSize(),
            resultado.getTotalElements(),
            resultado.getTotalPages()
        );
    }


    @Transactional
    public Produto receberEstoque(Long id, BigDecimal quantidade) {
        Produto produto = buscarPorId(id);
        produto.receberEstoque(quantidade);
        return produtoRepository.save(produto);
    }

    @Transactional
    public Produto retirarEstoque(Long id, BigDecimal quantidade) {
        Produto produto = buscarPorId(id);
        produto.retirarEstoque(quantidade);
        return produtoRepository.save(produto);
    }

    @Transactional
    public Produto ativar(Long id) {
        Produto produto = buscarPorId(id);
        produto.ativar();
        return produtoRepository.save(produto);
    }

    @Transactional
    public Produto inativar(Long id) {
        Produto produto = buscarPorId(id);
        produto.inativar();
        return produtoRepository.save(produto);
    }
}
