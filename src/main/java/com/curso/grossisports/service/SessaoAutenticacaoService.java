package com.curso.grossisports.service;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.MotivoRevogacaoRefreshToken;
import com.curso.grossisports.domain.SessaoRefreshToken;
import com.curso.grossisports.exception.CredenciaisInvalidasException;
import com.curso.grossisports.exception.RefreshTokenInvalidoException;
import com.curso.grossisports.repository.ContaAcessoRepository;
import com.curso.grossisports.repository.SessaoRefreshTokenRepository;
import com.curso.grossisports.security.RefreshTokenProperties;
import com.curso.grossisports.security.TokenAcesso;
import com.curso.grossisports.security.TokenService;
import com.curso.grossisports.security.TokensAutenticacao;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Ciclo de vida das sessões renováveis (Aula 16).
 *
 * <p>O refresh token é opaco: 32 bytes de {@link SecureRandom} em Base64
 * URL sem padding. O banco guarda apenas o hash SHA-256 em hexadecimal,
 * nunca o valor em claro. Cada login abre uma família ({@code familiaId});
 * a renovação gira o token mantendo a mesma família, e qualquer
 * reutilização revoga a família inteira.</p>
 */
@Service
public class SessaoAutenticacaoService {

    private static final int BYTES_REFRESH_TOKEN = 32;
    private static final String ALGORITMO_HASH = "SHA-256";

    private final SessaoRefreshTokenRepository sessaoRepository;
    private final ContaAcessoRepository contaRepository;
    private final TokenService tokenService;
    private final RefreshTokenProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessaoAutenticacaoService(
        SessaoRefreshTokenRepository sessaoRepository,
        ContaAcessoRepository contaRepository,
        TokenService tokenService,
        RefreshTokenProperties properties,
        Clock clock) {

        this.sessaoRepository = sessaoRepository;
        this.contaRepository = contaRepository;
        this.tokenService = tokenService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Abre a sessão no login: cria a primeira geração da família e emite
     * o par de tokens. A conta é reconferida para não abrir sessão de
     * conta bloqueada ou pessoa inativa.
     */
    @Transactional
    public TokensAutenticacao iniciarSessao(
        Authentication authentication) {

        ContaAcesso conta = contaRepository
            .findByLoginIgnoreCase(authentication.getName())
            .orElseThrow(() -> new CredenciaisInvalidasException(
                "Login ou senha inválidos"));

        if (!conta.podeAutenticar()) {
            throw new CredenciaisInvalidasException(
                "Login ou senha inválidos");
        }

        Instant agora = clock.instant();
        String familiaId = UUID.randomUUID().toString();
        String refreshToken = gerarRefreshToken();

        SessaoRefreshToken sessao = new SessaoRefreshToken(
            conta,
            familiaId,
            hash(refreshToken),
            agora,
            agora,
            agora.plus(properties.absoluteExpiration()));
        sessaoRepository.save(sessao);

        TokenAcesso acesso = tokenService.emitir(
            authentication, familiaId);

        return new TokensAutenticacao(
            acesso, refreshToken, sessao.getExpiraEm());
    }

    /**
     * Renova a sessão girando o refresh token.
     *
     * <p>{@code noRollbackFor} garante que a revogação gravada nos
     * caminhos de reutilização, expiração e conta inativa sobreviva à
     * {@link RefreshTokenInvalidoException} que encerra a operação — sem
     * isso, a própria exceção desfaria a evidência de segurança.</p>
     */
    @Transactional(noRollbackFor = RefreshTokenInvalidoException.class)
    public TokensAutenticacao renovar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshTokenInvalidoException();
        }

        SessaoRefreshToken sessao = sessaoRepository
            .buscarPorHashParaAtualizacao(hash(refreshToken))
            .orElseThrow(RefreshTokenInvalidoException::new);

        Instant agora = clock.instant();

        // Reutilização: a geração já foi trocada. Um token roubado e
        // reapresentado denuncia o vazamento; encerramos a família.
        if (sessao.foiUtilizada()) {
            revogarFamilia(sessao, agora,
                MotivoRevogacaoRefreshToken.REUTILIZACAO);
            throw new RefreshTokenInvalidoException();
        }

        if (sessao.foiRevogada()) {
            throw new RefreshTokenInvalidoException();
        }

        if (sessao.expirouAbsolutamente(agora)) {
            revogarFamilia(sessao, agora,
                MotivoRevogacaoRefreshToken.EXPIRACAO_ABSOLUTA);
            throw new RefreshTokenInvalidoException();
        }

        if (sessao.expirouPorInatividade(
            agora, properties.idleTimeout())) {

            revogarFamilia(sessao, agora,
                MotivoRevogacaoRefreshToken.INATIVIDADE);
            throw new RefreshTokenInvalidoException();
        }

        ContaAcesso conta = sessao.getConta();

        if (!conta.podeAutenticar()) {
            revogarFamilia(sessao, agora,
                MotivoRevogacaoRefreshToken.CONTA_INATIVA);
            throw new RefreshTokenInvalidoException();
        }

        sessao.marcarUtilizada(agora);
        sessaoRepository.save(sessao);

        String novoRefreshToken = gerarRefreshToken();

        SessaoRefreshToken nova = new SessaoRefreshToken(
            conta,
            sessao.getFamiliaId(),
            hash(novoRefreshToken),
            agora,
            agora,
            sessao.getExpiraEm());
        sessaoRepository.save(nova);

        TokenAcesso acesso = tokenService.emitir(
            autenticacaoDe(conta), sessao.getFamiliaId());

        return new TokensAutenticacao(
            acesso, novoRefreshToken, nova.getExpiraEm());
    }

    /**
     * Encerra a família de refresh tokens identificada pelo token
     * informado (logout local). É idempotente e silenciosa: um token
     * desconhecido, já revogado ou expirado não gera erro nem revela se
     * a sessão existia. O motivo registrado é {@code LOGOUT}.
     */
    @Transactional
    public void encerrarSessao(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        sessaoRepository
            .buscarPorHashParaAtualizacao(hash(refreshToken))
            .ifPresent(sessao -> sessaoRepository.revogarFamilia(
                sessao.getFamiliaId(),
                clock.instant(),
                MotivoRevogacaoRefreshToken.LOGOUT));
    }

    /**
     * Encerra todas as famílias de uma conta (logout global). O motivo
     * registrado é {@code LOGOUT_GLOBAL}. Também é idempotente.
     */
    @Transactional
    public void encerrarTodasSessoes(String login) {
        contaRepository.findByLoginIgnoreCase(login)
            .ifPresent(conta -> sessaoRepository.revogarTodasDaConta(
                conta.getId(),
                clock.instant(),
                MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL));
    }

    private void revogarFamilia(
        SessaoRefreshToken sessao,
        Instant agora,
        MotivoRevogacaoRefreshToken motivo) {

        sessaoRepository.revogarFamilia(
            sessao.getFamiliaId(), agora, motivo);
    }

    /**
     * Reconstrói a autenticação a partir da conta para reemitir o JWT na
     * renovação sem exigir a senha novamente. As authorities derivam dos
     * perfis persistidos, nunca de dados do cliente.
     */
    private Authentication autenticacaoDe(ContaAcesso conta) {
        List<SimpleGrantedAuthority> autoridades = conta.getPerfis()
            .stream()
            .map(perfil ->
                new SimpleGrantedAuthority("ROLE_" + perfil.name()))
            .toList();

        return new UsernamePasswordAuthenticationToken(
            conta.getLogin(), null, autoridades);
    }

    private String gerarRefreshToken() {
        byte[] bytes = new byte[BYTES_REFRESH_TOKEN];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest =
                MessageDigest.getInstance(ALGORITMO_HASH);
            byte[] resumo = digest.digest(
                token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "Algoritmo SHA-256 indisponível", exception);
        }
    }
}
