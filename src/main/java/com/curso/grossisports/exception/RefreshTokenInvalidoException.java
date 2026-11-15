package com.curso.grossisports.exception;

/**
 * Refresh token ausente, desconhecido, expirado, revogado, reutilizado
 * ou pertencente a conta inativa (Aula 16).
 *
 * <p>Estende {@link CredenciaisInvalidasException} para reaproveitar a
 * resposta {@code 401} genérica já tratada pelo
 * {@code ApiExceptionHandler}. A mensagem é a mesma em todos os casos:
 * não revela se o token existe, se expirou, se já foi utilizado ou se
 * foi revogado, reduzindo a informação oferecida a quem testa
 * credenciais roubadas.</p>
 */
public class RefreshTokenInvalidoException
    extends CredenciaisInvalidasException {

    public RefreshTokenInvalidoException() {
        super("Refresh token inválido ou expirado");
    }
}
