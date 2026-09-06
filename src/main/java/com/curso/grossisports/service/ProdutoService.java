package com.curso.grossisports.service;

import com.curso.grossisports.domain.Fornecedor;
import com.curso.grossisports.domain.GrupoProduto;
import com.curso.grossisports.domain.Produto;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.FornecedorRepository;
import com.curso.grossisports.repository.GrupoProdutoRepository;
import com.curso.grossisports.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

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
}
