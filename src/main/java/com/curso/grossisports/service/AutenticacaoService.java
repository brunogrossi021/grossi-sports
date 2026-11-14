package com.curso.grossisports.service;

import com.curso.grossisports.exception.CredenciaisInvalidasException;
import com.curso.grossisports.security.TokenAcesso;
import com.curso.grossisports.security.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Autentica login e senha reaproveitando o {@link AuthenticationManager}
 * (Aula 13), sem duplicar a comparação BCrypt.
 */
@Service
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AutenticacaoService(
        AuthenticationManager authenticationManager,
        TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    public TokenAcesso autenticar(String login, String senha) {
        try {
            Authentication authentication =
                authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken
                        .unauthenticated(login, senha));

            return tokenService.emitir(authentication);
        } catch (AuthenticationException exception) {
            throw new CredenciaisInvalidasException(
                "Login ou senha inválidos");
        }
    }
}
