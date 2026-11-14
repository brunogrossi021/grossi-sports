package com.curso.grossisports.service;

import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.Status;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.PessoaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PessoaServiceUnitTest {

    private static final String CPF = "52998224725";
    private static final String EMAIL = "aluna.teste@example.com";

    @Mock
    private PessoaRepository repository;

    @InjectMocks
    private PessoaService service;

    @Test
    void deveCadastrarPessoaQuandoCpfEEmailSaoUnicos() {
        when(repository.existsByCpf(CPF)).thenReturn(false);
        when(repository.existsByEmailIgnoreCase(EMAIL))
            .thenReturn(false);
        when(repository.save(any(Pessoa.class)))
            .thenAnswer(invocacao -> invocacao.getArgument(0));

        Pessoa criada = service.cadastrar(
            "Aluna de Teste", EMAIL, CPF);

        assertAll(
            () -> assertEquals("Aluna de Teste", criada.getNome()),
            () -> assertEquals(EMAIL, criada.getEmail()),
            () -> assertEquals(CPF, criada.getCpf()),
            () -> assertEquals(Status.ATIVO, criada.getStatus()));
        verify(repository).save(any(Pessoa.class));
    }

    @Test
    void deveRejeitarCadastroComCpfDuplicado() {
        when(repository.existsByCpf(CPF)).thenReturn(true);

        RecursoDuplicadoException erro = assertThrows(
            RecursoDuplicadoException.class,
            () -> service.cadastrar("Aluna", EMAIL, CPF));

        assertEquals("CPF já cadastrado", erro.getMessage());
        verify(repository, never()).save(any(Pessoa.class));
    }

    @Test
    void deveRejeitarCadastroComEmailDuplicado() {
        when(repository.existsByCpf(CPF)).thenReturn(false);
        when(repository.existsByEmailIgnoreCase(EMAIL))
            .thenReturn(true);

        RecursoDuplicadoException erro = assertThrows(
            RecursoDuplicadoException.class,
            () -> service.cadastrar("Aluna", EMAIL, CPF));

        assertEquals("E-mail já cadastrado", erro.getMessage());
    }

    @Test
    void deveBuscarPessoaPorId() {
        Pessoa pessoa = novaPessoa();
        when(repository.findById(1L)).thenReturn(Optional.of(pessoa));

        assertSame(pessoa, service.buscarPorId(1L));
    }

    @Test
    void deveFalharAoBuscarPessoaInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException erro = assertThrows(
            RecursoNaoEncontradoException.class,
            () -> service.buscarPorId(99L));

        assertEquals("Pessoa não encontrada", erro.getMessage());
    }

    @Test
    void deveAlterarPessoaQuandoOsDadosSaoUnicos() {
        Pessoa pessoa = novaPessoa();
        when(repository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.existsByCpfAndIdNot(CPF, 1L))
            .thenReturn(false);
        when(repository.existsByEmailIgnoreCaseAndIdNot(EMAIL, 1L))
            .thenReturn(false);
        when(repository.save(pessoa)).thenReturn(pessoa);

        Pessoa alterada = service.alterar(
            1L, "Aluna Alterada", EMAIL, CPF);

        assertEquals("Aluna Alterada", alterada.getNome());
    }

    @Test
    void deveRejeitarAlteracaoComCpfDeOutraPessoa() {
        Pessoa pessoa = novaPessoa();
        when(repository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.existsByCpfAndIdNot(CPF, 1L))
            .thenReturn(true);

        assertThrows(
            RecursoDuplicadoException.class,
            () -> service.alterar(1L, "Aluna", EMAIL, CPF));
    }

    @Test
    void deveRejeitarAlteracaoComEmailDeOutraPessoa() {
        Pessoa pessoa = novaPessoa();
        when(repository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.existsByCpfAndIdNot(CPF, 1L))
            .thenReturn(false);
        when(repository.existsByEmailIgnoreCaseAndIdNot(EMAIL, 1L))
            .thenReturn(true);

        assertThrows(
            RecursoDuplicadoException.class,
            () -> service.alterar(1L, "Aluna", EMAIL, CPF));
    }

    @Test
    void deveAlterarStatusParaAtivoEInativo() {
        Pessoa pessoa = novaPessoa();
        when(repository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.save(pessoa)).thenReturn(pessoa);

        pessoa.inativar();
        assertEquals(
            Status.ATIVO,
            service.alterarStatus(1L, Status.ATIVO).getStatus());

        assertEquals(
            Status.INATIVO,
            service.alterarStatus(1L, Status.INATIVO).getStatus());
    }

    @Test
    void deveAtivarEInativarPorAtalho() {
        Pessoa pessoa = novaPessoa();
        when(repository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.save(pessoa)).thenReturn(pessoa);

        assertEquals(Status.ATIVO, service.ativar(1L).getStatus());
        assertEquals(
            Status.INATIVO, service.inativar(1L).getStatus());
    }

    @Test
    void deveRejeitarStatusNulo() {
        IllegalArgumentException erro = assertThrows(
            IllegalArgumentException.class,
            () -> service.alterarStatus(1L, null));

        assertEquals("Status é obrigatório", erro.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void devePesquisarAplicandoTodosOsFiltros() {
        Pessoa pessoa = novaPessoa();
        Page<Pessoa> pagina = new PageImpl<>(
            List.of(pessoa), PageRequest.of(0, 10), 1);
        when(repository.findAll(
            ArgumentMatchers.<Specification<Pessoa>>any(),
            ArgumentMatchers.<Pageable>any()))
            .thenReturn(pagina);

        PageResponse<Pessoa> resposta = service.pesquisar(
            "  aluna  ", " " + CPF + " ", Status.ATIVO,
            0, 10, "nome", "desc");

        assertAll(
            () -> assertEquals(1, resposta.content().size()),
            () -> assertEquals(0, resposta.page()),
            () -> assertEquals(10, resposta.size()),
            () -> assertEquals(1, resposta.totalElements()));
    }

    @Test
    void devePesquisarSemFiltros() {
        when(repository.findAll(
            ArgumentMatchers.<Specification<Pessoa>>any(),
            ArgumentMatchers.<Pageable>any()))
            .thenReturn(new PageImpl<>(List.of()));

        PageResponse<Pessoa> resposta = service.pesquisar(
            null, null, null, 0, 10, "id", "asc");

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
            "Aluna de Teste",
            EMAIL,
            CPF,
            LocalDate.of(2026, 10, 29));
    }
}
