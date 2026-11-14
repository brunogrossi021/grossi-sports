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
class ColaboradorServiceUnitTest {

    private static final String CPF = "52998224725";
    private static final String EMAIL = "colaborador.teste@example.com";
    private static final LocalDate ADMISSAO =
        LocalDate.of(2026, 10, 29);

    @Mock
    private ColaboradorRepository colaboradorRepository;

    @Mock
    private PessoaRepository pessoaRepository;

    @InjectMocks
    private ColaboradorService service;

    @Test
    void deveCadastrarColaboradorComMatriculaNormalizada() {
        Pessoa pessoa = novaPessoa();
        when(pessoaRepository.findById(1L))
            .thenReturn(Optional.of(pessoa));
        when(colaboradorRepository.existsByPessoaId(1L))
            .thenReturn(false);
        when(colaboradorRepository.existsByMatricula("MAT-001"))
            .thenReturn(false);
        when(colaboradorRepository.save(any(Colaborador.class)))
            .thenAnswer(invocacao -> invocacao.getArgument(0));

        Colaborador criado = service.cadastrar(
            1L, "  MAT-001  ", FuncaoColaborador.VENDEDOR,
            ADMISSAO);

        assertAll(
            () -> assertSame(pessoa, criado.getPessoa()),
            () -> assertEquals("MAT-001", criado.getMatricula()),
            () -> assertEquals(
                FuncaoColaborador.VENDEDOR, criado.getFuncao()));
    }

    @Test
    void deveRejeitarColaboradorParaPessoaInexistente() {
        when(pessoaRepository.findById(99L))
            .thenReturn(Optional.empty());

        RecursoNaoEncontradoException erro = assertThrows(
            RecursoNaoEncontradoException.class,
            () -> service.cadastrar(
                99L, "MAT-001", FuncaoColaborador.VENDEDOR,
                ADMISSAO));

        assertEquals("Pessoa não encontrada", erro.getMessage());
        verify(colaboradorRepository, never())
            .save(any(Colaborador.class));
    }

    @Test
    void deveRejeitarColaboradorParaPessoaInativa() {
        Pessoa pessoa = novaPessoa();
        pessoa.inativar();
        when(pessoaRepository.findById(1L))
            .thenReturn(Optional.of(pessoa));

        IllegalArgumentException erro = assertThrows(
            IllegalArgumentException.class,
            () -> service.cadastrar(
                1L, "MAT-001", FuncaoColaborador.VENDEDOR,
                ADMISSAO));

        assertEquals("Pessoa deve estar ativa", erro.getMessage());
    }

    @Test
    void deveRejeitarColaboradorDuplicado() {
        Pessoa pessoa = novaPessoa();
        when(pessoaRepository.findById(1L))
            .thenReturn(Optional.of(pessoa));
        when(colaboradorRepository.existsByPessoaId(1L))
            .thenReturn(true);

        RecursoDuplicadoException erro = assertThrows(
            RecursoDuplicadoException.class,
            () -> service.cadastrar(
                1L, "MAT-001", FuncaoColaborador.VENDEDOR,
                ADMISSAO));

        assertEquals(
            "Pessoa já possui cadastro de colaborador",
            erro.getMessage());
    }

    @Test
    void deveRejeitarMatriculaDuplicada() {
        Pessoa pessoa = novaPessoa();
        when(pessoaRepository.findById(1L))
            .thenReturn(Optional.of(pessoa));
        when(colaboradorRepository.existsByPessoaId(1L))
            .thenReturn(false);
        when(colaboradorRepository.existsByMatricula("MAT-001"))
            .thenReturn(true);

        RecursoDuplicadoException erro = assertThrows(
            RecursoDuplicadoException.class,
            () -> service.cadastrar(
                1L, "MAT-001", FuncaoColaborador.VENDEDOR,
                ADMISSAO));

        assertEquals("Matrícula já cadastrada", erro.getMessage());
    }

    @Test
    void deveBuscarColaboradorPorId() {
        Colaborador colaborador = novoColaborador();
        when(colaboradorRepository.findOneById(1L))
            .thenReturn(Optional.of(colaborador));

        assertSame(colaborador, service.buscarPorId(1L));
    }

    @Test
    void deveFalharAoBuscarColaboradorInexistente() {
        when(colaboradorRepository.findOneById(99L))
            .thenReturn(Optional.empty());

        RecursoNaoEncontradoException erro = assertThrows(
            RecursoNaoEncontradoException.class,
            () -> service.buscarPorId(99L));

        assertEquals(
            "Colaborador não encontrado", erro.getMessage());
    }

    @Test
    void deveAlterarDadosDoColaborador() {
        Colaborador colaborador = novoColaborador();
        when(colaboradorRepository.findOneById(1L))
            .thenReturn(Optional.of(colaborador));
        when(colaboradorRepository.existsByMatriculaAndIdNot(
            "MAT-002", 1L)).thenReturn(false);
        when(colaboradorRepository.save(colaborador))
            .thenReturn(colaborador);

        Colaborador alterado = service.alterar(
            1L, "  MAT-002  ", FuncaoColaborador.GERENTE);

        assertAll(
            () -> assertEquals("MAT-002", alterado.getMatricula()),
            () -> assertEquals(
                FuncaoColaborador.GERENTE, alterado.getFuncao()));
    }

    @Test
    void deveRejeitarAlteracaoComMatriculaDeOutroColaborador() {
        Colaborador colaborador = novoColaborador();
        when(colaboradorRepository.findOneById(1L))
            .thenReturn(Optional.of(colaborador));
        when(colaboradorRepository.existsByMatriculaAndIdNot(
            "MAT-002", 1L)).thenReturn(true);

        assertThrows(
            RecursoDuplicadoException.class,
            () -> service.alterar(
                1L, "MAT-002", FuncaoColaborador.GERENTE));
    }

    @Test
    void devePesquisarAplicandoTodosOsFiltros() {
        Colaborador colaborador = novoColaborador();
        Page<Colaborador> pagina =
            new PageImpl<>(List.of(colaborador));
        when(colaboradorRepository.findAll(
            ArgumentMatchers.<Specification<Colaborador>>any(),
            ArgumentMatchers.<Pageable>any()))
            .thenReturn(pagina);

        PageResponse<Colaborador> resposta = service.pesquisar(
            "  colaborador  ", FuncaoColaborador.VENDEDOR,
            Status.ATIVO, 0, 10, "matricula", "asc");

        assertEquals(1, resposta.content().size());
    }

    @Test
    void devePesquisarSemFiltros() {
        when(colaboradorRepository.findAll(
            ArgumentMatchers.<Specification<Colaborador>>any(),
            ArgumentMatchers.<Pageable>any()))
            .thenReturn(new PageImpl<>(List.of()));

        PageResponse<Colaborador> resposta = service.pesquisar(
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

    private Colaborador novoColaborador() {
        return new Colaborador(
            novaPessoa(), "MAT-001",
            FuncaoColaborador.VENDEDOR, ADMISSAO);
    }

    private Pessoa novaPessoa() {
        return new Pessoa(
            "Colaborador de Teste",
            EMAIL,
            CPF,
            ADMISSAO);
    }
}
