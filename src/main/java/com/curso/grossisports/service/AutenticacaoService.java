package com.curso.grossisports.service;

import com.curso.grossisports.exception.CredenciaisInvalidasException;
import com.curso.grossisports.security.TokensAutenticacao;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Autentica login e senha reaproveitando o {@link AuthenticationManager}
 * (Aula 13), sem duplicar a comparação BCrypt.
 *
 * <p>Aula 16: após a autenticação, delega a abertura da sessão renovável
 * ao {@link SessaoAutenticacaoService}, que emite o par de tokens
 * (acesso + refresh).</p>
 */
@Service
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final SessaoAutenticacaoService sessaoService;

    public AutenticacaoService(
        AuthenticationManager authenticationManager,
        SessaoAutenticacaoService sessaoService) {

        this.authenticationManager = authenticationManager;
        this.sessaoService = sessaoService;
    }

    public TokensAutenticacao autenticar(String login, String senha) {
        try {
            Authentication authentication =
                authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken
                        .unauthenticated(login, senha));

            return sessaoService.iniciarSessao(authentication);
        } catch (AuthenticationException exception) {
            throw new CredenciaisInvalidasException(
                "Login ou senha inválidos");
        }
    }

    /**
     * Renova a sessão girando o refresh token (Aula 16). Delega ao
     * serviço de sessões, que aplica rotação, limites e detecção de
     * reutilização dentro da transação própria.
     */
    public TokensAutenticacao renovar(String refreshToken) {
        return sessaoService.renovar(refreshToken);
    }

    /** Logout local: revoga a família do refresh token informado. */
    public void encerrarSessao(String refreshToken) {
        sessaoService.encerrarSessao(refreshToken);
    }

    /** Logout global: revoga todas as sessões da conta autenticada. */
    public void encerrarTodasSessoes(String login) {
        sessaoService.encerrarTodasSessoes(login);
    }
}
