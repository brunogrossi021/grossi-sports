package com.curso.grossisports.service;

import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.domain.Cliente;
import com.curso.grossisports.domain.Colaborador;
import com.curso.grossisports.domain.FuncaoColaborador;
import com.curso.grossisports.domain.ItemVenda;
import com.curso.grossisports.domain.Produto;
import com.curso.grossisports.domain.Status;
import com.curso.grossisports.domain.StatusVenda;
import com.curso.grossisports.domain.Venda;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.ClienteRepository;
import com.curso.grossisports.repository.ColaboradorRepository;
import com.curso.grossisports.repository.ProdutoRepository;
import com.curso.grossisports.repository.VendaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Service
public class VendaService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
        "id",
        "dataAbertura",
        "status"
    );

    private final VendaRepository vendaRepository;
    private final ClienteRepository clienteRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final ProdutoRepository produtoRepository;

    public VendaService(
        VendaRepository vendaRepository,
        ClienteRepository clienteRepository,
        ColaboradorRepository colaboradorRepository,
        ProdutoRepository produtoRepository
    ) {
        this.vendaRepository = vendaRepository;
        this.clienteRepository = clienteRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Venda abrirVenda(Long clienteId, Long colaboradorId) {
        Cliente cliente = clienteRepository.findOneById(clienteId)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Cliente não encontrado"));

        Colaborador colaborador = colaboradorRepository
            .findOneById(colaboradorId)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Colaborador não encontrado"));

        validarParticipantes(cliente, colaborador);

        Venda venda = new Venda(cliente, colaborador);

        return vendaRepository.save(venda);
    }

    @Transactional(readOnly = true)
    public Venda buscarPorId(Long id) {
        return vendaRepository.findOneById(id)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Venda não encontrada"));
    }

    @Transactional(readOnly = true)
    public PageResponse<Venda> pesquisar(
        StatusVenda status,
        Long clienteId,
        Long vendedorId,
        LocalDate dataInicial,
        LocalDate dataFinal,
        int pagina,
        int tamanho,
        String ordenarPor,
        String direcao
    ) {
        validarParametrosPaginacao(
            pagina, tamanho, ordenarPor, direcao);

        if (dataInicial != null
            && dataFinal != null
            && dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException(
                "Data inicial não pode ser posterior à data final");
        }

        Sort sort = "desc".equalsIgnoreCase(direcao)
            ? Sort.by(ordenarPor).descending()
            : Sort.by(ordenarPor).ascending();

        Pageable pageable = PageRequest.of(pagina, tamanho, sort);

        Specification<Venda> filtros =
            (root, query, cb) -> cb.conjunction();

        if (status != null) {
            filtros = filtros.and((root, query, cb) ->
                cb.equal(root.get("status"), status));
        }

        if (clienteId != null) {
            filtros = filtros.and((root, query, cb) ->
                cb.equal(root.get("cliente").get("id"), clienteId));
        }

        if (vendedorId != null) {
            filtros = filtros.and((root, query, cb) ->
                cb.equal(
                    root.get("colaborador").get("id"),
                    vendedorId));
        }

        if (dataInicial != null) {
            filtros = filtros.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(
                    root.get("dataAbertura"),
                    dataInicial.atStartOfDay()));
        }

        if (dataFinal != null) {
            filtros = filtros.and((root, query, cb) ->
                cb.lessThan(
                    root.get("dataAbertura"),
                    dataFinal.plusDays(1).atStartOfDay()));
        }

        Page<Venda> resultado =
            vendaRepository.findAll(filtros, pageable);

        return new PageResponse<>(
            resultado.getContent(),
            resultado.getNumber(),
            resultado.getSize(),
            resultado.getTotalElements(),
            resultado.getTotalPages()
        );
    }

    @Transactional
    public Venda adicionarItem(
        Long vendaId,
        Long produtoId,
        BigDecimal quantidade
    ) {
        Venda venda = buscarPorId(vendaId);

        Produto produto = produtoRepository.findById(produtoId)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Produto não encontrado"));

        if (produto.getStatus() != Status.ATIVO) {
            throw new IllegalArgumentException(
                "Produto deve estar ativo");
        }

        venda.adicionarItem(produto, quantidade);

        return venda;
    }

    @Transactional
    public Venda removerItem(Long vendaId, Long itemId) {
        Venda venda = buscarPorId(vendaId);
        venda.removerItem(itemId);
        return venda;
    }

    @Transactional
    public Venda cancelar(Long vendaId) {
        Venda venda = buscarPorId(vendaId);
        venda.cancelar();
        return venda;
    }

    @Transactional
    public Venda finalizar(Long vendaId) {
        Venda venda = buscarPorId(vendaId);

        validarParticipantes(
            venda.getCliente(),
            venda.getColaborador());

        for (ItemVenda item : venda.getItens()) {
            Produto produto = item.getProduto();

            if (produto.getStatus() != Status.ATIVO) {
                throw new IllegalArgumentException(
                    "Todos os produtos da venda devem estar ativos");
            }

            produto.retirarEstoque(item.getQuantidade());
        }

        venda.finalizar();

        return venda;
    }

    private void validarParticipantes(
        Cliente cliente,
        Colaborador colaborador
    ) {
        if (cliente.getPessoa().getStatus() != Status.ATIVO) {
            throw new IllegalArgumentException(
                "Cliente deve estar ativo");
        }

        if (colaborador.getPessoa().getStatus() != Status.ATIVO) {
            throw new IllegalArgumentException(
                "Vendedor deve estar ativo");
        }

        if (colaborador.getFuncao() != FuncaoColaborador.VENDEDOR
            && colaborador.getFuncao() != FuncaoColaborador.GERENTE) {
            throw new IllegalArgumentException(
                "Colaborador não possui função habilitada para vender");
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
