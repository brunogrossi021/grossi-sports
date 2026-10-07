
package com.curso.grossisports.service;

import com.curso.grossisports.api.dto.PageResponse;
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

    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return produtoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PageResponse<Produto> listarPaginado(
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
                "Campo de ordenação inválido");
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
        Page<Produto> resultado = produtoRepository.findAll(pageable);

        return new PageResponse<>(
            resultado.getContent(),
            resultado.getNumber(),
            resultado.getSize(),
            resultado.getTotalElements(),
            resultado.getTotalPages()
        );
    }
}
