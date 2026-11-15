package com.curso.grossisports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Evento de segurança estruturado (Aula 17).
 *
 * <p>Uma linha responde a quatro perguntas de investigação: o que
 * ocorreu ({@code tipo}), quando ({@code ocorridoEm}), quem iniciou
 * ({@code atorContaId}) e quem foi afetado ({@code alvoContaId}). Os
 * identificadores de conta são <strong>valores históricos</strong>, não
 * associações JPA: a trilha não depende do ciclo de vida posterior da
 * conta.</p>
 *
 * <p>Nunca são registrados senha, hash BCrypt, tokens de acesso ou de
 * renovação, o hash do refresh token, o cabeçalho {@code Authorization}
 * nem o corpo da requisição. Em falhas de login, o login original
 * também não aparece: apenas o {@code identificadorHash} de correlação.
 * A estrutura é fechada (tipo enumerado, ids numéricos, hash validado e
 * origem sanitizada) para reduzir <em>log injection</em>.</p>
 *
 * <p>Esta trilha serve para investigação; não oferece não repúdio
 * criptográfico.</p>
 */
@Entity
@Table(name = "evento_seguranca")
public class EventoSeguranca {

    private static final Pattern HASH_SHA256 =
        Pattern.compile("[0-9a-f]{64}");

    private static final int TAMANHO_MAXIMO_ORIGEM = 45;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoEventoSeguranca tipo;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(name = "ator_conta_id")
    private Long atorContaId;

    @Column(name = "alvo_conta_id")
    private Long alvoContaId;

    @Column(name = "identificador_hash", length = 64)
    private String identificadorHash;

    @Column(name = "endereco_origem", length = 45)
    private String enderecoOrigem;

    protected EventoSeguranca() {
    }

    private EventoSeguranca(
        TipoEventoSeguranca tipo,
        Instant ocorridoEm,
        Long atorContaId,
        Long alvoContaId,
        String identificadorHash,
        String enderecoOrigem) {

        this.tipo = Objects.requireNonNull(
            tipo, "Tipo do evento é obrigatório");
        this.ocorridoEm = Objects.requireNonNull(
            ocorridoEm, "Instante do evento é obrigatório");
        this.atorContaId = validarId(atorContaId, "ator");
        this.alvoContaId = validarId(alvoContaId, "alvo");
        this.identificadorHash = validarHash(identificadorHash);
        this.enderecoOrigem = validarOrigem(enderecoOrigem);
    }

    /**
     * Cria um evento validado. Os campos de correlação e origem são
     * opcionais; quando presentes, precisam respeitar o formato
     * esperado.
     */
    public static EventoSeguranca registrar(
        TipoEventoSeguranca tipo,
        Instant ocorridoEm,
        Long atorContaId,
        Long alvoContaId,
        String identificadorHash,
        String enderecoOrigem) {

        return new EventoSeguranca(
            tipo,
            ocorridoEm,
            atorContaId,
            alvoContaId,
            identificadorHash,
            enderecoOrigem);
    }

    private static Long validarId(Long valor, String papel) {
        if (valor != null && valor <= 0) {
            throw new IllegalArgumentException(
                "O identificador de " + papel + " deve ser positivo");
        }

        return valor;
    }

    private static String validarHash(String valor) {
        if (valor == null) {
            return null;
        }

        if (!HASH_SHA256.matcher(valor).matches()) {
            throw new IllegalArgumentException(
                "O identificador deve ser um hash SHA-256 hexadecimal");
        }

        return valor;
    }

    private static String validarOrigem(String valor) {
        if (valor == null) {
            return null;
        }

        if (valor.length() > TAMANHO_MAXIMO_ORIGEM
            || valor.contains("\r")
            || valor.contains("\n")) {

            throw new IllegalArgumentException(
                "Endereço de origem inválido");
        }

        return valor;
    }

    public Long getId() {
        return id;
    }

    public TipoEventoSeguranca getTipo() {
        return tipo;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public Long getAtorContaId() {
        return atorContaId;
    }

    public Long getAlvoContaId() {
        return alvoContaId;
    }

    public String getIdentificadorHash() {
        return identificadorHash;
    }

    public String getEnderecoOrigem() {
        return enderecoOrigem;
    }
}
