package com.curso.grossisports;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.PerfilAcesso;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.StatusContaAcesso;
import com.curso.grossisports.repository.ContaAcessoRepository;
import com.curso.grossisports.repository.PessoaRepository;
import com.curso.grossisports.service.AuditoriaSegurancaService;
import com.curso.grossisports.service.ContaAcessoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Falha de auditoria obrigatória reverte a ação sensível (Aula 17).
 *
 * <p>A trilha participa da mesma transação da alteração administrativa.
 * Quando a gravação do evento falha, a alteração de negócio também não é
 * confirmada — a implementação falha fechada. A classe não é
 * {@code @Transactional}: o rollback precisa ser real para que a
 * ausência da mudança seja observada em uma leitura nova.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class AuditoriaFalhaReverteAcaoTest {

    private static final String SENHA = "uma-senha-didatica-2026";

    @Autowired
    private ContaAcessoService contaAcessoService;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private AuditoriaSegurancaService auditoria;

    private final List<Long> contasCriadas = new ArrayList<>();
    private final List<Long> pessoasCriadas = new ArrayList<>();

    @AfterEach
    void limpar() {
        for (Long contaId : contasCriadas) {
            jdbcTemplate.update(
                "DELETE FROM conta_acesso_perfil "
                    + "WHERE conta_acesso_id = ?", contaId);
            jdbcTemplate.update(
                "DELETE FROM conta_acesso WHERE id = ?", contaId);
        }

        for (Long pessoaId : pessoasCriadas) {
            jdbcTemplate.update(
                "DELETE FROM pessoa WHERE id = ?", pessoaId);
        }

        contasCriadas.clear();
        pessoasCriadas.clear();
    }

    @Test
    @WithMockUser(username = "admin.falha.auditoria",
        roles = "ADMINISTRADOR")
    void falhaDeAuditoriaReverteAlteracaoDeStatus() {
        salvarContaAdmin("admin.falha.auditoria");
        ContaAcesso alvo = salvarConta("alvo.falha.auditoria");

        doThrow(new IllegalStateException("auditoria indisponível"))
            .when(auditoria)
            .registrar(any(), any(), any(), any(), any());

        assertThrows(
            IllegalStateException.class,
            () -> contaAcessoService.alterarStatus(
                alvo.getId(),
                StatusContaAcesso.BLOQUEADA,
                "admin.falha.auditoria",
                "127.0.0.1"));

        ContaAcesso recarregado = contaRepository
            .findById(alvo.getId())
            .orElseThrow();

        assertEquals(StatusContaAcesso.ATIVA, recarregado.getStatus());
    }

    private ContaAcesso salvarConta(String login) {
        Pessoa pessoa = pessoaRepository.saveAndFlush(new Pessoa(
            "Pessoa " + login,
            login + "." + Math.abs(System.nanoTime())
                + "@example.com",
            gerarCpf(Math.abs(System.nanoTime())),
            LocalDate.of(2026, 1, 1)));

        ContaAcesso conta = contaRepository.saveAndFlush(
            new ContaAcesso(
                pessoa, login, passwordEncoder.encode(SENHA)));

        contasCriadas.add(conta.getId());
        pessoasCriadas.add(pessoa.getId());

        return conta;
    }

    private ContaAcesso salvarContaAdmin(String login) {
        ContaAcesso conta = salvarConta(login);
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        return contaRepository.saveAndFlush(conta);
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
