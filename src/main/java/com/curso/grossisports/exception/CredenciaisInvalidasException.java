package com.curso.grossisports.exception;

/**
 * Credenciais inválidas no login (Aula 13).
 *
 * <p>Usada de forma genérica para login inexistente, senha incorreta ou
 * conta bloqueada, reduzindo a enumeração de contas.</p>
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String message) {
        super(message);
    }
}
