package com.curso.grossisports.service;

import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.domain.Cliente;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.Status;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.ClienteRepository;
import com.curso.grossisports.repository.PessoaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceUnitTest {

    private static final String CPF = "52998224725";
    private static final String EMAIL = "cliente.teste@example.com";

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PessoaRepository pessoaRepository;

    @InjectMocks
    private ClienteService service;

    @Test
    void deveCadastrarClienteQuandoPessoaAtivaESemCadastro() {
        Pessoa pessoa = novaPessoa();
        when(pessoaRepository.findById(1L))
            .thenReturn(Optional.of(pessoa));
        when(clienteRepository.existsByPessoaId(1L))
            .thenReturn(false);
        when(clienteRepository.save(any(Cliente.class)))
            .thenAnswer(invocacao -> invocacao.getArgument(0));

        Cliente criado = service.cadastrar(1L, "11 99999-0000");

        assertAll(
            () -> assertSame(pessoa, criado.getPessoa()),
            () -> assertEquals("11 99999-0000",
                criado.getTelefone()));
    }

    @Test
    void deveRejeitarClienteParaPessoaInexistente() {
        when(pessoaRepository.findById(99L))
            .thenReturn(Optional.empty());

        RecursoNaoEncontradoException erro = assertThrows(
            RecursoNaoEncontradoException.class,
            () -> service.cadastrar(99L, "11 99999-0000"));

        assertEquals("Pessoa não encontrada", erro.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void deveRejeitarClienteParaPessoaInativa() {
        Pessoa pessoa = novaPessoa();
        pessoa.inativar();
        when(pessoaRepository.findById(1L))
            .thenReturn(Optional.of(pessoa));

        IllegalArgumentException erro = assertThrows(
            IllegalArgumentException.class,
            () -> service.cadastrar(1L, "11 99999-0000"));

        assertEquals("Pessoa deve estar ativa", erro.getMessage());
    }

    @Test
    void deveRejeitarClienteDuplicado() {
        Pessoa pessoa = novaPessoa();
        when(pessoaRepository.findById(1L))
            .thenReturn(Optional.of(pessoa));
        when(clienteRepository.existsByPessoaId(1L))
            .thenReturn(true);

        RecursoDuplicadoException erro = assertThrows(
            RecursoDuplicadoException.class,
            () -> service.cadastrar(1L, "11 99999-0000"));

        assertEquals(
            "Pessoa já possui cadastro de cliente",
            erro.getMessage());
    }

    @Test
    void deveBuscarClientePorId() {
        Cliente cliente = new Cliente(novaPessoa(), "11 99999-0000");
        when(clienteRepository.findOneById(1L))
            .thenReturn(Optional.of(cliente));

        assertSame(cliente, service.buscarPorId(1L));
    }

    @Test
    void deveFalharAoBuscarClienteInexistente() {
        when(clienteRepository.findOneById(99L))
            .thenReturn(Optional.empty());

        RecursoNaoEncontradoException erro = assertThrows(
            RecursoNaoEncontradoException.class,
            () -> service.buscarPorId(99L));

        assertEquals("Cliente não encontrado", erro.getMessage());
    }

    @Test
    void deveAlterarTelefoneDoCliente() {
        Cliente cliente = new Cliente(novaPessoa(), "11 99999-0000");
        when(clienteRepository.findOneById(1L))
            .thenReturn(Optional.of(cliente));
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        Cliente alterado = service.alterar(1L, "11 98888-1111");

        assertEquals("11 98888-1111", alterado.getTelefone());
    }

    @Test
    void devePesquisarAplicandoTodosOsFiltros() {
        Cliente cliente = new Cliente(novaPessoa(), "11 99999-0000");
        Page<Cliente> pagina = new PageImpl<>(List.of(cliente));
        when(clienteRepository.findAll(
            ArgumentMatchers.<Specification<Cliente>>any(),
            ArgumentMatchers.<Pageable>any()))
            .thenReturn(pagina);

        PageResponse<Cliente> resposta = service.pesquisar(
            "  cliente  ", " " + CPF + " ", Status.ATIVO,
            0, 10, "pessoa.nome", "asc");

        assertEquals(1, resposta.content().size());
    }

    @Test
    void devePesquisarSemFiltros() {
        when(clienteRepository.findAll(
            ArgumentMatchers.<Specification<Cliente>>any(),
            ArgumentMatchers.<Pageable>any()))
            .thenReturn(new PageImpl<>(List.of()));

        PageResponse<Cliente> resposta = service.pesquisar(
            null, null, null, 0, 10, "id", "desc");

        assertTrue(resposta.content().isEmpty());
    }

    @Test
    void deveRejeitarParametrosDePaginacaoInvalidos() {
        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> service.pesquisar(
                    null, null, null, -1, 10, "id", "asc")),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> service.pesquisar(
                    null, null, null, 0, 0, "id", "asc")),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> service.pesquisar(
                    null, null, null, 0, 101, "id", "asc")),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> service.pesquisar(
                    null, null, null, 0, 10, "inexistente", "asc")),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> service.pesquisar(
                    null, null, null, 0, 10, "id", "lado")));
    }

    private Pessoa novaPessoa() {
        return new Pessoa(
            "Cliente de Teste",
            EMAIL,
            CPF,
            LocalDate.of(2026, 10, 29));
    }
}
