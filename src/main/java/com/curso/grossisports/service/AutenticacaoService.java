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
 *
 * <p>Aula 17: antes de tocar em BCrypt, consulta o
 * {@link ProtecaoAutenticacaoService}. Se houver bloqueio temporário
 * ativo, recusa de imediato e registra o evento. Em caso de credencial
 * inválida, registra a falha <strong>antes</strong> de lançar a resposta
 * genérica — o método não é transacional justamente para que a transação
 * de {@code registrarFalha} confirme antes de a exceção propagar.</p>
 */
@Service
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final SessaoAutenticacaoService sessaoService;
    private final ProtecaoAutenticacaoService protecaoService;

    public AutenticacaoService(
        AuthenticationManager authenticationManager,
        SessaoAutenticacaoService sessaoService,
        ProtecaoAutenticacaoService protecaoService) {

        this.authenticationManager = authenticationManager;
        this.sessaoService = sessaoService;
        this.protecaoService = protecaoService;
    }

    public TokensAutenticacao autenticar(
        String login, String senha, String enderecoOrigem) {

        if (!protecaoService.permitirTentativa(login, enderecoOrigem)) {
            throw new CredenciaisInvalidasException(
                "Login ou senha inválidos");
        }

        try {
            Authentication authentication =
                authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken
                        .unauthenticated(login, senha));

            TokensAutenticacao tokens =
                sessaoService.iniciarSessao(authentication);

            protecaoService.registrarSucesso(login, enderecoOrigem);

            return tokens;
        } catch (AuthenticationException exception) {
            protecaoService.registrarFalha(login, enderecoOrigem);

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
