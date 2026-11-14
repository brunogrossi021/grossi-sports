package com.curso.grossisports.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração externa do administrador inicial (Aula 14).
 *
 * <p>Desativada por padrão ({@code enabled=false}). Quando habilitada,
 * todas as demais propriedades passam a ser obrigatórias. Os valores
 * vêm do arquivo {@code .env} (fora do Git) por meio das variáveis
 * {@code INITIAL_ADMIN_*}.</p>
 */
@ConfigurationProperties(prefix = "app.security.initial-admin")
public record AdministradorInicialProperties(
    boolean enabled,
    String nome,
    String email,
    String cpf,
    String login,
    String senha) {
}
