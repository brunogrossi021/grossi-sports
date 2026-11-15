package com.curso.grossisports.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Confirma o binding real das propriedades no contexto (Aula 16).
 *
 * <p>As durações vêm do {@code application.properties} (valores padrão
 * {@code PT30M}/{@code PT8H}) e são convertidas para {@link Duration}.
 * Se o binding ou a validação falhassem, o contexto nem subiria.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenPropertiesBindingTest {

    @Autowired
    private RefreshTokenProperties properties;

    @Test
    void deveCarregarDuracoesDoApplicationProperties() {
        assertEquals(Duration.ofMinutes(30), properties.idleTimeout());
        assertEquals(
            Duration.ofHours(8), properties.absoluteExpiration());
    }
}
