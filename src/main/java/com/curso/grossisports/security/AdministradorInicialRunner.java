package com.curso.grossisports.security;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.service.AdministradorInicialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Executa o provisionamento do administrador inicial apenas quando
 * {@code app.security.initial-admin.enabled=true} (Aula 14).
 *
 * <p>Após a primeira execução, recomenda-se desativar a flag. O serviço
 * é idempotente, mas desligar a flag reduz superfície e ambiguidade
 * operacional.</p>
 */
@Component
@ConditionalOnProperty(
    name = "app.security.initial-admin.enabled",
    havingValue = "true")
public class AdministradorInicialRunner implements ApplicationRunner {

    private static final Logger LOG =
        LoggerFactory.getLogger(AdministradorInicialRunner.class);

    private final AdministradorInicialService service;
    private final AdministradorInicialProperties properties;

    public AdministradorInicialRunner(
        AdministradorInicialService service,
        AdministradorInicialProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<ContaAcesso> criada =
            service.criarSeNecessario(properties);

        if (criada.isPresent()) {
            LOG.warn(
                "Administrador inicial criado com login '{}'. "
                    + "Desative INITIAL_ADMIN_ENABLED após a primeira "
                    + "execução.",
                criada.get().getLogin());
        } else {
            LOG.info(
                "Administrador inicial não criado: já existe "
                    + "administrador ativo.");
        }
    }
}
