package com.curso.grossisports.service;

import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.domain.Cliente;
import com.curso.grossisports.domain.Colaborador;
import com.curso.grossisports.domain.FuncaoColaborador;
import com.curso.grossisports.domain.GrupoProduto;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.Produto;
import com.curso.grossisports.domain.StatusVenda;
import com.curso.grossisports.domain.Venda;
import com.curso.grossisports.repository.GrupoProdutoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class VendaServiceTest {

    private static final String PREFIXO_EMAIL = "venda.teste.";
    private static final String PREFIXO_CODIGO = "VENDA-TESTE-";
    private static final String PREFIXO_GRUPO = "Grupo Venda Teste";

    @Autowired
    private PessoaService pessoaService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ColaboradorService colaboradorService;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private VendaService vendaService;

    @Autowired
    private GrupoProdutoRepository grupoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void limparDadosDoCenario() {
        String filtroEmail = PREFIXO_EMAIL + "%";

        jdbcTemplate.update("""
            DELETE FROM item_venda
            WHERE venda_id IN (
                SELECT v.id
                FROM venda v
                JOIN cliente c ON c.id = v.cliente_id
                JOIN pessoa p ON p.id = c.pessoa_id
                WHERE p.email LIKE ?
            )
            """, filtroEmail);

        jdbcTemplate.update("""
            DELETE FROM venda
            WHERE cliente_id IN (
                SELECT c.id
                FROM cliente c
                JOIN pessoa p ON p.id = c.pessoa_id
                WHERE p.email LIKE ?
            )
            """, filtroEmail);

        jdbcTemplate.update("""
            DELETE FROM cliente
            WHERE pessoa_id IN (
                SELECT id FROM pessoa WHERE email LIKE ?
            )
            """, filtroEmail);

        jdbcTemplate.update("""
            DELETE FROM colaborador
            WHERE pessoa_id IN (
                SELECT id FROM pessoa WHERE email LIKE ?
            )
            """, filtroEmail);

        jdbcTemplate.update(
            "DELETE FROM pessoa WHERE email LIKE ?",
            filtroEmail);

        jdbcTemplate.update(
            "DELETE FROM produto WHERE codigo_barras LIKE ?",
            PREFIXO_CODIGO + "%");

        jdbcTemplate.update(
            "DELETE FROM grupo_produto WHERE nome LIKE ?",
            PREFIXO_GRUPO + "%");
    }

    @Test
    void deveFinalizarVendaEBaixarEstoque() {
        Participantes participantes = criarParticipantes();
        Produto produto = criarProduto("OK", "5.000");

        Venda venda = vendaService.abrirVenda(
            participantes.cliente().getId(),
            participantes.vendedor().getId()
        );

        vendaService.adicionarItem(
            venda.getId(),
            produto.getId(),
            new BigDecimal("2.000")
        );

        vendaService.finalizar(venda.getId());

        assertEquals(
            "FINALIZADA",
            statusNoBanco(venda.getId()));

        assertEquals(
            0,
            new BigDecimal("3.000").compareTo(
                saldoNoBanco(produto.getId())));
    }

    @Test
    void deveDesfazerTodasAsBaixasQuandoUmItemNaoTemEstoque() {
        Participantes participantes = criarParticipantes();
        Produto primeiro = criarProduto("ROLLBACK-A", "5.000");
        Produto segundo = criarProduto("ROLLBACK-B", "1.000");

        Venda venda = vendaService.abrirVenda(
            participantes.cliente().getId(),
            participantes.vendedor().getId()
        );

        vendaService.adicionarItem(
            venda.getId(),
            primeiro.getId(),
            new BigDecimal("2.000")
        );

        vendaService.adicionarItem(
            venda.getId(),
            segundo.getId(),
            new BigDecimal("2.000")
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> vendaService.finalizar(venda.getId())
        );

        assertEquals(
            0,
            new BigDecimal("5.000").compareTo(
                saldoNoBanco(primeiro.getId())),
            "O primeiro produto não pode ter baixa parcial"
        );

        assertEquals(
            0,
            new BigDecimal("1.000").compareTo(
                saldoNoBanco(segundo.getId())),
            "O segundo produto deve permanecer com o saldo original"
        );

        assertEquals(
            "ABERTA",
            statusNoBanco(venda.getId()),
            "A venda deve permanecer aberta após o rollback"
        );
    }

    @Test
    void naoDeveAdicionarProdutoInativo() {
        Participantes participantes = criarParticipantes();
        Produto produto = criarProduto("INATIVO", "5.000");

        produtoService.inativar(produto.getId());

        Venda venda = vendaService.abrirVenda(
            participantes.cliente().getId(),
            participantes.vendedor().getId()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> vendaService.adicionarItem(
                venda.getId(),
                produto.getId(),
                new BigDecimal("1.000"))
        );
    }

    @Test
    void naoDeveAdicionarMesmoProdutoDuasVezes() {
        Participantes participantes = criarParticipantes();
        Produto produto = criarProduto("DUPLICADO", "5.000");

        Venda venda = vendaService.abrirVenda(
            participantes.cliente().getId(),
            participantes.vendedor().getId()
        );

        vendaService.adicionarItem(
            venda.getId(),
            produto.getId(),
            new BigDecimal("1.000")
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> vendaService.adicionarItem(
                venda.getId(),
                produto.getId(),
                new BigDecimal("1.000"))
        );
    }

    @Test
    void naoDeveAbrirVendaParaVendedorInativo() {
        Participantes participantes = criarParticipantes();

        pessoaService.inativar(
            participantes.vendedor().getPessoa().getId());

        assertThrows(
            IllegalArgumentException.class,
            () -> vendaService.abrirVenda(
                participantes.cliente().getId(),
                participantes.vendedor().getId())
        );
    }

    @Test
    void devePesquisarVendaPorClienteEStatus() {
        Participantes participantes = criarParticipantes();
        Produto produto = criarProduto("PESQUISA", "5.000");

        Venda venda = vendaService.abrirVenda(
            participantes.cliente().getId(),
            participantes.vendedor().getId()
        );

        vendaService.adicionarItem(
            venda.getId(),
            produto.getId(),
            BigDecimal.ONE
        );

        PageResponse<Venda> resultado = vendaService.pesquisar(
            StatusVenda.ABERTA,
            participantes.cliente().getId(),
            null,
            null,
            null,
            0,
            10,
            "dataAbertura",
            "desc"
        );

        assertEquals(1L, resultado.totalElements());
        assertEquals(venda.getId(), resultado.content().get(0).getId());
    }

    @Test
    void deveImpedirVendaAcimaDoSaldoEmFinalizacoesConcorrentes() throws Exception {
        Participantes participantes = criarParticipantes();
        Produto produto = criarProduto("CONCORRENTE", "5.000");
        Venda primeira = criarVendaComItem(participantes, produto, "4.000");
        Venda segunda = criarVendaComItem(participantes, produto, "4.000");

        CountDownLatch inicioSimultaneo = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<ResultadoFinalizacao> primeiroResultado = executor.submit(
                () -> finalizarAposSinal(primeira.getId(), inicioSimultaneo));
            Future<ResultadoFinalizacao> segundoResultado = executor.submit(
                () -> finalizarAposSinal(segunda.getId(), inicioSimultaneo));

            inicioSimultaneo.countDown();

            List<ResultadoFinalizacao> resultados = List.of(
                primeiroResultado.get(10, TimeUnit.SECONDS),
                segundoResultado.get(10, TimeUnit.SECONDS));

            assertEquals(
                1,
                resultados.stream()
                    .filter(ResultadoFinalizacao::sucesso)
                    .count());
            assertEquals(
                1,
                resultados.stream()
                    .filter(resultado -> !resultado.sucesso())
                    .count());
            assertTrue(resultados.stream()
                .filter(resultado -> !resultado.sucesso())
                .allMatch(resultado ->
                    "Saldo de estoque insuficiente"
                        .equals(resultado.mensagem())));
        }

        assertEquals(
            new BigDecimal("1.000"),
            saldoNoBanco(produto.getId()));

        List<StatusVenda> estados = List.of(
            vendaService.buscarPorId(primeira.getId()).getStatus(),
            vendaService.buscarPorId(segunda.getId()).getStatus());
        assertEquals(
            1,
            estados.stream()
                .filter(StatusVenda.FINALIZADA::equals)
                .count());
        assertEquals(
            1,
            estados.stream()
                .filter(StatusVenda.ABERTA::equals)
                .count());
    }

    private Venda criarVendaComItem(
        Participantes participantes,
        Produto produto,
        String quantidade
    ) {
        Venda venda = vendaService.abrirVenda(
            participantes.cliente().getId(),
            participantes.vendedor().getId()
        );

        vendaService.adicionarItem(
            venda.getId(),
            produto.getId(),
            new BigDecimal(quantidade)
        );

        return venda;
    }

    private ResultadoFinalizacao finalizarAposSinal(
        Long vendaId,
        CountDownLatch inicioSimultaneo
    ) throws InterruptedException {
        inicioSimultaneo.await();

        try {
            vendaService.finalizar(vendaId);
            return new ResultadoFinalizacao(true, null);
        } catch (IllegalArgumentException exception) {
            return new ResultadoFinalizacao(false, exception.getMessage());
        }
    }

    private BigDecimal saldoNoBanco(Long produtoId) {
        return jdbcTemplate.queryForObject(
            "SELECT saldo_estoque FROM produto WHERE id = ?",
            BigDecimal.class,
            produtoId
        );
    }

    private String statusNoBanco(Long vendaId) {
        return jdbcTemplate.queryForObject(
            "SELECT status FROM venda WHERE id = ?",
            String.class,
            vendaId
        );
    }

    private Participantes criarParticipantes() {
        long semente = System.nanoTime();
        String sufixo = String.valueOf(semente);

        Pessoa pessoaCliente = pessoaService.cadastrar(
            "Cliente Venda Teste",
            PREFIXO_EMAIL + "cliente." + sufixo + "@example.com",
            gerarCpf(semente)
        );

        Pessoa pessoaVendedor = pessoaService.cadastrar(
            "Vendedor Venda Teste",
            PREFIXO_EMAIL + "vendedor." + sufixo + "@example.com",
            gerarCpf(semente + 7919)
        );

        Cliente cliente = clienteService.cadastrar(
            pessoaCliente.getId(),
            "17999999999"
        );

        Colaborador vendedor = colaboradorService.cadastrar(
            pessoaVendedor.getId(),
            "V" + Math.floorMod(semente, 1_000_000_000L),
            FuncaoColaborador.VENDEDOR,
            LocalDate.now()
        );

        return new Participantes(cliente, vendedor);
    }

    private Produto criarProduto(String identificador, String saldo) {
        GrupoProduto grupo = grupoRepository.save(
            new GrupoProduto(PREFIXO_GRUPO + " " + identificador)
        );

        Produto produto = new Produto(
            PREFIXO_CODIGO + identificador + "-" + System.nanoTime(),
            "Produto de teste " + identificador,
            new BigDecimal(saldo),
            new BigDecimal("10.00"),
            LocalDate.now()
        );

        return produtoService.cadastrar(
            produto,
            grupo.getId(),
            null
        );
    }

    private String gerarCpf(long semente) {
        String base = "%09d".formatted(
            Math.floorMod(semente, 1_000_000_000L));

        if (digitosRepetidos(base)) {
            base = "123456789";
        }

        int primeiro = digitoCpf(base, 10);

        return base + primeiro + digitoCpf(base + primeiro, 11);
    }

    private int digitoCpf(String base, int peso) {
        int soma = 0;

        for (char c : base.toCharArray()) {
            soma += Character.getNumericValue(c) * peso--;
        }

        int resto = soma % 11;

        return resto < 2 ? 0 : 11 - resto;
    }

    private boolean digitosRepetidos(String valor) {
        for (int i = 1; i < valor.length(); i++) {
            if (valor.charAt(i) != valor.charAt(0)) {
                return false;
            }
        }

        return true;
    }

    private record Participantes(
        Cliente cliente,
        Colaborador vendedor
    ) {
    }

    private record ResultadoFinalizacao(
        boolean sucesso,
        String mensagem
    ) {
    }
}
