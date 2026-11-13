package com.curso.grossisports.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PessoaTest {

    @Test
    void deveNormalizarEmailEPreservarCpfValido() {
        Pessoa pessoa = new Pessoa(
            " Ana Souza ",
            " ANA@EXAMPLE.COM ",
            "52998224725",
            LocalDate.of(2026, 11, 10)
        );

        assertEquals("Ana Souza", pessoa.getNome());
        assertEquals("ana@example.com", pessoa.getEmail());
        assertEquals("52998224725", pessoa.getCpf());
        assertEquals(Status.ATIVO, pessoa.getStatus());
    }

    @Test
    void naoDeveAceitarCpfComDigitoVerificadorIncorreto() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Pessoa(
                "Ana",
                "ana@example.com",
                "52998224724",
                LocalDate.now())
        );
    }

    @Test
    void naoDeveAceitarEmailInvalido() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Pessoa(
                "Ana",
                "email-invalido",
                "52998224725",
                LocalDate.now())
        );
    }

    @Test
    void deveAtivarEInativarPessoa() {
        Pessoa pessoa = new Pessoa(
            "Ana",
            "ana@example.com",
            "52998224725",
            LocalDate.now()
        );

        pessoa.inativar();
        assertEquals(Status.INATIVO, pessoa.getStatus());

        pessoa.ativar();
        assertEquals(Status.ATIVO, pessoa.getStatus());
    }

    @Test
    void deveNormalizarEmailNaAlteracaoDeDados() {
        Pessoa pessoa = new Pessoa(
            "Ana",
            "ana@example.com",
            "52998224725",
            LocalDate.now()
        );

        pessoa.alterarDados(
            "Ana Souza",
            " NOVO@EXAMPLE.COM ",
            "11144477735"
        );

        assertEquals("Ana Souza", pessoa.getNome());
        assertEquals("novo@example.com", pessoa.getEmail());
        assertEquals("11144477735", pessoa.getCpf());
    }
}
