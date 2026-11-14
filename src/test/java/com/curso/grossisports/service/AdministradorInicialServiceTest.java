package com.curso.grossisports.service;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.PerfilAcesso;
import com.curso.grossisports.domain.StatusContaAcesso;
import com.curso.grossisports.repository.ContaAcessoRepository;
import com.curso.grossisports.security.AdministradorInicialProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bootstrap do administrador inicial (Aula 14). Não depende de banco
 * vazio: cada teste estabelece a precondição dentro da própria
 * transação (que é revertida ao final).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdministradorInicialServiceTest {

    private static final String SENHA = "senha-forte-inicial-2026";

    @Autowired
    private AdministradorInicialService service;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Test
    void deveCriarAdministradorQuandoNaoExiste() {
        garantirAusenciaDeAdministrador();

        Optional<ContaAcesso> criada =
            service.criarSeNecessario(propriedades("adm.inicial"));

        assertTrue(criada.isPresent());

        ContaAcesso conta = criada.orElseThrow();

        assertTrue(conta.possuiPerfil(PerfilAcesso.OPERADOR));
        assertTrue(conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR));
        assertNotEquals(SENHA, conta.getSenhaHash());
        assertTrue(conta.getSenhaHash().startsWith("{bcrypt}"));
    }

    @Test
    void naoDeveDuplicarAdministradorExistente() {
        garantirAusenciaDeAdministrador();

        assertTrue(service.criarSeNecessario(
            propriedades("adm.um")).isPresent());
        assertTrue(service.criarSeNecessario(
            propriedades("adm.dois")).isEmpty());

        assertEquals(
            1L,
            contaRepository.contarPorPerfilEStatus(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA));
    }

    @Test
    void deveFalharComConfiguracaoIncompleta() {
        garantirAusenciaDeAdministrador();

        AdministradorInicialProperties incompleta =
            new AdministradorInicialProperties(
                true,
                "Administrador Inicial",
                "",
                "52998224725",
                "adm.incompleto",
                SENHA);

        IllegalStateException erro = assertThrows(
            IllegalStateException.class,
            () -> service.criarSeNecessario(incompleta));

        assertTrue(erro.getMessage().contains("INITIAL_ADMIN_EMAIL"));
    }

    private void garantirAusenciaDeAdministrador() {
        contaRepository.deleteAll();
        contaRepository.flush();
    }

    private AdministradorInicialProperties propriedades(String login) {
        long semente = Math.abs(System.nanoTime());

        return new AdministradorInicialProperties(
            true,
            "Administrador Inicial",
            login + "." + semente + "@example.com",
            gerarCpf(semente),
            login,
            SENHA);
    }

    private String gerarCpf(long semente) {
        String base = "%09d".formatted(
            Math.floorMod(semente, 1_000_000_000L));

        if (digitosRepetidos(base)) {
            base = "123456789";
        }

        int primeiro = digitoCpf(base, 10);

        return base + primeiro + digitoCpf(base + primeiro, 11);
    }

    private int digitoCpf(String base, int peso) {
        int soma = 0;

        for (char c : base.toCharArray()) {
            soma += Character.getNumericValue(c) * peso--;
        }

        int resto = soma % 11;

        return resto < 2 ? 0 : 11 - resto;
    }

    private boolean digitosRepetidos(String valor) {
        for (int i = 1; i < valor.length(); i++) {
            if (valor.charAt(i) != valor.charAt(0)) {
                return false;
            }
        }

        return true;
    }
}
