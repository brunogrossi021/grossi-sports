package com.curso.grossisports;

import com.curso.grossisports.security.AdministradorInicialProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Política de segurança (Aulas 12–14).
 *
 * <p>Aula 14: o claim {@code perfis} do JWT é convertido em authorities
 * {@code ROLE_*} e a autorização passa a diferenciar OPERADOR e
 * ADMINISTRADOR. A regra final é {@code denyAll()}: nenhuma rota nova se
 * torna pública por esquecimento.</p>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AdministradorInicialProperties.class)
public class SecurityConfig {

    private static final String CLAIM_PERFIS = "perfis";
    private static final String PREFIXO_ROLE = "ROLE_";

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories
            .createDelegatingPasswordEncoder();
    }

    /**
     * Converte o claim {@code perfis} (ex.: {@code ["OPERADOR"]}) nas
     * authorities {@code ROLE_OPERADOR} compreendidas pelo Spring
     * Security. O conversor padrão procuraria scopes, não perfis.
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(CLAIM_PERFIS);
        authoritiesConverter.setAuthorityPrefix(PREFIXO_ROLE);

        JwtAuthenticationConverter authenticationConverter =
            new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(
            authoritiesConverter);

        return authenticationConverter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationConverter jwtAuthenticationConverter)
        throws Exception {

        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                // Públicas: infraestrutura e documentação.
                .requestMatchers(
                    "/api/health",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-ui.html",
                    "/swagger-ui/**")
                .permitAll()
                // Login é público: o cliente ainda não possui token.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/autenticacao/login")
                .permitAll()
                // Administração de contas: somente ADMINISTRADOR.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/contas-acesso")
                .hasRole("ADMINISTRADOR")
                .requestMatchers(
                    "/api/contas-acesso/*/perfis",
                    "/api/contas-acesso/*/status")
                .hasRole("ADMINISTRADOR")
                // Exclusão administrativa de recursos.
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/grupos-produtos/**")
                .hasRole("ADMINISTRADOR")
                // Demais operações da API: cotidiano do operador.
                .requestMatchers("/api/**")
                .hasAnyRole("OPERADOR", "ADMINISTRADOR")
                // Negar por padrão o que não foi classificado.
                //
                // Decisão sobre /error: NÃO é liberado. Verificamos que o
                // encaminhamento de erro não é submetido a estas regras
                // nesta configuração: uma rota /api/** inexistente com
                // token válido responde 404 (e não 403), e os erros da API
                // são tratados por @RestControllerAdvice. Liberar /error
                // seria desnecessário e ampliaria a superfície pública.
                .anyRequest()
                .denyAll())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                    jwtAuthenticationConverter)))
            .build();
    }
}
