package com.curso.grossisports.domain;

/**
 * Vocabulário fechado dos eventos de segurança (Aula 17).
 *
 * <p>O tipo é um {@code enum}, nunca texto livre fornecido pelo
 * usuário. Isso reduz a superfície de <em>log injection</em>, impede
 * que uma mensagem arbitrária entre na trilha e torna as consultas
 * administrativas previsíveis. Cada constante responde à pergunta "o
 * que ocorreu?".</p>
 */
public enum TipoEventoSeguranca {
    LOGIN_SUCESSO,
    LOGIN_FALHA,
    LOGIN_RECUSADO_BLOQUEIO,
    BLOQUEIO_TEMPORARIO_ATIVADO,
    CONTA_CRIADA,
    SENHA_ALTERADA,
    PERFIS_ALTERADOS,
    STATUS_ALTERADO,
    REFRESH_SUCESSO,
    REFRESH_FALHA,
    LOGOUT,
    LOGOUT_GLOBAL
}
