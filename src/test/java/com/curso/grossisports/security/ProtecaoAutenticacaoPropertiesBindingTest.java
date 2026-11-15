package com.curso.grossisports.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Confirma o binding real da política de proteção (Aula 17).
 *
 * <p>Os valores vêm do {@code application.properties} (5 falhas, 15
 * minutos de janela e 15 minutos de bloqueio). Se o binding ou a
 * validação falhassem, o contexto nem subiria.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ProtecaoAutenticacaoPropertiesBindingTest {

    @Autowired
    private ProtecaoAutenticacaoProperties properties;

    @Test
    void deveCarregarPoliticaDoApplicationProperties() {
        assertEquals(5, properties.maxFailedAttempts());
        assertEquals(
            Duration.ofMinutes(15), properties.failureWindow());
        assertEquals(
            Duration.ofMinutes(15), properties.lockDuration());
    }
}
