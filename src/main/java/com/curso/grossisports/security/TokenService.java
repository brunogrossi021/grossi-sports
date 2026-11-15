package com.curso.grossisports.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Emite o JWT a partir de uma autenticação já validada (Aulas 13 e 16).
 *
 * <p>Os perfis vêm exclusivamente das authorities do objeto autenticado,
 * nunca do JSON enviado pelo cliente. A partir da Aula 16 o token carrega
 * o claim {@code sid} com o identificador da família de refresh tokens,
 * permitindo correlacionar o acesso à sessão renovável que o originou.</p>
 */
@Service
public class TokenService {

    private static final String PREFIXO_PERFIL = "ROLE_";
    private static final String CLAIM_SESSAO = "sid";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenService(
        JwtEncoder encoder,
        JwtProperties properties,
        Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public TokenAcesso emitir(
        Authentication authentication, String sessaoId) {

        Instant emitidoEm = clock.instant();
        Instant expiraEm = emitidoEm.plus(properties.expiration());

        List<String> perfis = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(valor -> valor.startsWith(PREFIXO_PERFIL))
            .map(valor -> valor.substring(PREFIXO_PERFIL.length()))
            .sorted()
            .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .issuedAt(emitidoEm)
            .expiresAt(expiraEm)
            .subject(authentication.getName())
            .id(UUID.randomUUID().toString())
            .claim("perfis", perfis)
            .claim(CLAIM_SESSAO, sessaoId)
            .build();

        String valor = encoder
            .encode(JwtEncoderParameters.from(claims))
            .getTokenValue();

        return new TokenAcesso(
            valor,
            expiraEm,
            properties.expiration().toSeconds());
    }
}
