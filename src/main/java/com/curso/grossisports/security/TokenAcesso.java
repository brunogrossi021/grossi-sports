package com.curso.grossisports.security;

import java.time.Instant;

/**
 * Resultado da emissão de um token (Aula 13).
 */
public record TokenAcesso(
    String valor,
    Instant expiraEm,
    long duracaoSegundos) {
}
