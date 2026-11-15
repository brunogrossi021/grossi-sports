package com.curso.grossisports.security;

import java.time.Instant;

/**
 * Par de tokens devolvido no login e na renovação (Aula 16).
 *
 * <p>O token de acesso continua sendo o JWT de curta duração; o refresh
 * token é opaco e só aparece em claro neste retorno — o servidor
 * persiste apenas o hash SHA-256. {@code refreshTokenExpiraEm} é o
 * limite absoluto da família, que não muda na rotação.</p>
 */
public record TokensAutenticacao(
    TokenAcesso acesso,
    String refreshToken,
    Instant refreshTokenExpiraEm) {
}
