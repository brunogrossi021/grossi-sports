package com.curso.grossisports.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração tipada da proteção contra tentativas automatizadas de
 * login (Aula 17).
 *
 * <p>Três parâmetros definem a política: quantas falhas consecutivas
 * ativam o bloqueio ({@code maxFailedAttempts}), em qual intervalo elas
 * são agrupadas ({@code failureWindow}) e por quanto tempo novas
 * tentativas são recusadas ({@code lockDuration}).</p>
 *
 * <p>A validação vive no construtor do record: a aplicação falha na
 * inicialização quando a política é nula, zero, negativa ou
 * inconsistente, em vez de executar sem um limite definido. Os valores
 * padrão (5 falhas, 15 minutos de janela e 15 minutos de bloqueio) são
 * didáticos e configuráveis, não uma recomendação universal.</p>
 */
@ConfigurationProperties(
    prefix = "app.security.authentication-protection")
public record ProtecaoAutenticacaoProperties(
    int maxFailedAttempts,
    Duration failureWindow,
    Duration lockDuration) {

    public ProtecaoAutenticacaoProperties {
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException(
                "O máximo de falhas deve ser positivo");
        }

        if (failureWindow == null
            || failureWindow.isZero()
            || failureWindow.isNegative()) {
            throw new IllegalArgumentException(
                "A janela de falhas deve ser positiva");
        }

        if (lockDuration == null
            || lockDuration.isZero()
            || lockDuration.isNegative()) {
            throw new IllegalArgumentException(
                "A duração do bloqueio deve ser positiva");
        }
    }
}
