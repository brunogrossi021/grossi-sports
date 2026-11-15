package com.curso.grossisports.domain;

/**
 * Motivos pelos quais uma sessão de refresh token deixa de renovar
 * (Aula 16).
 *
 * <p>O motivo é registrado junto da revogação para explicar, de forma
 * auditável, por que a família foi encerrada: logout local, logout
 * global, inatividade, expiração absoluta, detecção de reutilização ou
 * conta que não pode mais autenticar.</p>
 */
public enum MotivoRevogacaoRefreshToken {
    LOGOUT,
    LOGOUT_GLOBAL,
    INATIVIDADE,
    EXPIRACAO_ABSOLUTA,
    REUTILIZACAO,
    CONTA_INATIVA
}
