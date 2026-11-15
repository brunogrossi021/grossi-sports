package com.curso.grossisports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Contador persistente de falhas de login por identificador (Aula 17).
 *
 * <p>A chave é o hash SHA-256 do login normalizado, nunca o login em
 * texto aberto. O contador agrupa as falhas em uma janela de observação
 * ({@code janelaIniciadaEm}) e ativa um bloqueio temporário
 * ({@code bloqueadaAte}) quando o limite é atingido.</p>
 *
 * <p>O bloqueio temporário <strong>não</strong> é o
 * {@link StatusContaAcesso#BLOQUEADA}: aquele é uma decisão
 * administrativa persistente; este é uma contenção automática que se
 * dissolve sozinha ao fim do prazo.</p>
 */
@Entity
@Table(name = "protecao_autenticacao")
public class ProtecaoAutenticacao {

    private static final Pattern HASH_SHA256 =
        Pattern.compile("[0-9a-f]{64}");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "chave_login",
        nullable = false,
        unique = true,
        length = 64)
    private String chaveLogin;

    @Column(name = "falhas_consecutivas", nullable = false)
    private int falhasConsecutivas;

    @Column(name = "janela_iniciada_em")
    private Instant janelaIniciadaEm;

    @Column(name = "bloqueada_ate")
    private Instant bloqueadaAte;

    protected ProtecaoAutenticacao() {
    }

    public ProtecaoAutenticacao(String chaveLogin) {
        this.chaveLogin = validarHash(chaveLogin);
        this.falhasConsecutivas = 0;
    }

    /**
     * Registra uma falha e devolve {@code true} quando esta falha
     * ativou o bloqueio — sinalizando que um evento adicional deve ser
     * gravado.
     *
     * <p>Uma falha fora da janela reinicia a contagem. O bloqueio é
     * ativado exatamente quando o número de falhas alcança o máximo.</p>
     */
    public boolean registrarFalha(
        Instant agora,
        int maximoTentativas,
        Duration janela,
        Duration duracaoBloqueio) {

        Objects.requireNonNull(agora, "Instante é obrigatório");
        Objects.requireNonNull(janela, "Janela é obrigatória");
        Objects.requireNonNull(
            duracaoBloqueio, "Duração do bloqueio é obrigatória");

        if (maximoTentativas < 1) {
            throw new IllegalArgumentException(
                "O máximo de falhas deve ser positivo");
        }

        if (janelaIniciadaEm == null
            || !agora.isBefore(janelaIniciadaEm.plus(janela))) {

            falhasConsecutivas = 0;
            janelaIniciadaEm = agora;
        }

        falhasConsecutivas++;

        if (falhasConsecutivas >= maximoTentativas) {
            bloqueadaAte = agora.plus(duracaoBloqueio);
            return true;
        }

        return false;
    }

    /**
     * O bloqueio vale enquanto {@code agora} for estritamente anterior a
     * {@code bloqueadaAte}. Se o prazo termina exatamente em
     * {@code 12:23:00}, a conta já está liberada nesse instante.
     */
    public boolean estaBloqueada(Instant agora) {
        return bloqueadaAte != null && agora.isBefore(bloqueadaAte);
    }

    /** Limpa falhas, janela e bloqueio após uma autenticação válida. */
    public void registrarSucesso() {
        falhasConsecutivas = 0;
        janelaIniciadaEm = null;
        bloqueadaAte = null;
    }

    public Long getId() {
        return id;
    }

    public String getChaveLogin() {
        return chaveLogin;
    }

    public int getFalhasConsecutivas() {
        return falhasConsecutivas;
    }

    public Instant getJanelaIniciadaEm() {
        return janelaIniciadaEm;
    }

    public Instant getBloqueadaAte() {
        return bloqueadaAte;
    }

    private static String validarHash(String chaveLogin) {
        if (chaveLogin == null
            || !HASH_SHA256.matcher(chaveLogin).matches()) {

            throw new IllegalArgumentException(
                "A chave de login deve ser um hash SHA-256 hexadecimal");
        }

        return chaveLogin;
    }
}
