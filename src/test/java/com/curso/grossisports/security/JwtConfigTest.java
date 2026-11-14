package com.curso.grossisports.security;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Confirma que a chave HS256 obrigatória é validada na inicialização
 * (Aula 13): a aplicação deve falhar de forma clara quando o segredo
 * estiver ausente, não for Base64 ou tiver menos de 32 bytes.
 */
class JwtConfigTest {

    private final JwtConfig config = new JwtConfig();

    @Test
    void deveRejeitarSegredoAusente() {
        JwtProperties properties =
            new JwtProperties("https://grossi-sports.local",
                Duration.ofMinutes(30), null);

        IllegalStateException erro = assertThrows(
            IllegalStateException.class,
            () -> config.jwtSecretKey(properties));

        assertEquals(
            "JWT_SECRET não foi definido. Gere uma chave Base64 "
                + "de 256 bits e coloque no arquivo .env.",
            erro.getMessage());
    }

    @Test
    void deveRejeitarSegredoEmBranco() {
        JwtProperties properties =
            new JwtProperties("https://grossi-sports.local",
                Duration.ofMinutes(30), "   ");

        assertThrows(
            IllegalStateException.class,
            () -> config.jwtSecretKey(properties));
    }

    @Test
    void deveRejeitarSegredoForaDoBase64() {
        JwtProperties properties =
            new JwtProperties("https://grossi-sports.local",
                Duration.ofMinutes(30), "nao-e-base64-valido!");

        IllegalStateException erro = assertThrows(
            IllegalStateException.class,
            () -> config.jwtSecretKey(properties));

        assertEquals(
            "JWT_SECRET deve estar codificada em Base64",
            erro.getMessage());
    }

    @Test
    void deveRejeitarSegredoMenorQue32Bytes() {
        String curto = Base64.getEncoder()
            .encodeToString(new byte[16]);

        JwtProperties properties =
            new JwtProperties("https://grossi-sports.local",
                Duration.ofMinutes(30), curto);

        IllegalStateException erro = assertThrows(
            IllegalStateException.class,
            () -> config.jwtSecretKey(properties));

        assertEquals(
            "JWT_SECRET deve possuir pelo menos 32 bytes",
            erro.getMessage());
    }

    @Test
    void deveAceitarSegredoDe32Bytes() {
        byte[] bytes = new byte[32];
        String valido = Base64.getEncoder().encodeToString(bytes);

        JwtProperties properties =
            new JwtProperties("https://grossi-sports.local",
                Duration.ofMinutes(30), valido);

        SecretKey chave = config.jwtSecretKey(properties);

        assertEquals("HmacSHA256", chave.getAlgorithm());
        assertEquals(32, chave.getEncoded().length);
    }
}
