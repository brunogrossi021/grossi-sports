package com.curso.grossisports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(
    name = "colaborador",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_colaborador_matricula",
            columnNames = "matricula"
        )
    }
)
public class Colaborador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "pessoa_id",
        nullable = false,
        unique = true
    )
    private Pessoa pessoa;

    @Column(nullable = false, length = 20)
    private String matricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FuncaoColaborador funcao;

    @Column(name = "data_admissao", nullable = false)
    private LocalDate dataAdmissao;

    protected Colaborador() {
    }

    public Colaborador(
        Pessoa pessoa,
        String matricula,
        FuncaoColaborador funcao,
        LocalDate dataAdmissao
    ) {
        if (pessoa == null) {
            throw new IllegalArgumentException(
                "Pessoa é obrigatória"
            );
        }

        if (matricula == null || matricula.isBlank()) {
            throw new IllegalArgumentException(
                "Matrícula é obrigatória"
            );
        }

        if (funcao == null) {
            throw new IllegalArgumentException(
                "Função é obrigatória"
            );
        }

        if (dataAdmissao == null) {
            throw new IllegalArgumentException(
                "Data de admissão é obrigatória"
            );
        }

        this.pessoa = pessoa;
        this.matricula = matricula.trim();
        this.funcao = funcao;
        this.dataAdmissao = dataAdmissao;
    }

    public void alterarDados(
        String matricula,
        FuncaoColaborador funcao
    ) {
        if (matricula == null || matricula.isBlank()) {
            throw new IllegalArgumentException(
                "Matrícula é obrigatória"
            );
        }

        if (funcao == null) {
            throw new IllegalArgumentException(
                "Função é obrigatória"
            );
        }

        this.matricula = matricula.trim();
        this.funcao = funcao;
    }

    public Long getId() {
        return id;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public String getMatricula() {
        return matricula;
    }

    public FuncaoColaborador getFuncao() {
        return funcao;
    }

    public LocalDate getDataAdmissao() {
        return dataAdmissao;
    }
}
