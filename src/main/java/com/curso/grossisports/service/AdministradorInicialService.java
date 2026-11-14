package com.curso.grossisports.service;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.PerfilAcesso;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.StatusContaAcesso;
import com.curso.grossisports.repository.ContaAcessoRepository;
import com.curso.grossisports.repository.PessoaRepository;
import com.curso.grossisports.security.AdministradorInicialProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Provisiona a primeira conta administrativa a partir de configuração
 * externa (Aula 14).
 *
 * <p>É idempotente: se já existir administrador ativo, nada é criado.
 * Usa os repositórios diretamente (não os casos de uso anotados com
 * {@code @PreAuthorize}), pois roda na inicialização sem autenticação.
 * A pessoa e a conta são criadas na mesma transação; a senha é
 * armazenada apenas como hash BCrypt.</p>
 */
@Service
public class AdministradorInicialService {

    private final ContaAcessoRepository contaRepository;
    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContaAcessoService contaAcessoService;

    public AdministradorInicialService(
        ContaAcessoRepository contaRepository,
        PessoaRepository pessoaRepository,
        PasswordEncoder passwordEncoder,
        ContaAcessoService contaAcessoService) {
        this.contaRepository = contaRepository;
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
        this.contaAcessoService = contaAcessoService;
    }

    @Transactional
    public Optional<ContaAcesso> criarSeNecessario(
        AdministradorInicialProperties properties) {

        if (jaExisteAdministradorAtivo()) {
            return Optional.empty();
        }

        validar(properties);
        contaAcessoService.validarSenhaAberta(properties.senha());

        if (contaRepository.existsByLoginIgnoreCase(
            properties.login())) {
            throw new IllegalStateException(
                "Login do administrador inicial já cadastrado: "
                    + properties.login());
        }

        if (pessoaRepository.existsByCpf(properties.cpf())) {
            throw new IllegalStateException(
                "CPF do administrador inicial já cadastrado");
        }

        if (pessoaRepository.existsByEmailIgnoreCase(
            properties.email())) {
            throw new IllegalStateException(
                "E-mail do administrador inicial já cadastrado");
        }

        Pessoa pessoa = pessoaRepository.save(new Pessoa(
            properties.nome(),
            properties.email(),
            properties.cpf(),
            LocalDate.now()));

        ContaAcesso conta = new ContaAcesso(
            pessoa,
            properties.login(),
            passwordEncoder.encode(properties.senha()));

        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);

        return Optional.of(contaRepository.save(conta));
    }

    private boolean jaExisteAdministradorAtivo() {
        return contaRepository.contarPorPerfilEStatus(
            PerfilAcesso.ADMINISTRADOR,
            StatusContaAcesso.ATIVA) > 0;
    }

    private void validar(AdministradorInicialProperties properties) {
        if (properties == null) {
            throw new IllegalStateException(
                "Configuração do administrador inicial ausente");
        }

        exigir(properties.nome(), "INITIAL_ADMIN_NAME");
        exigir(properties.email(), "INITIAL_ADMIN_EMAIL");
        exigir(properties.cpf(), "INITIAL_ADMIN_CPF");
        exigir(properties.login(), "INITIAL_ADMIN_LOGIN");
        exigir(properties.senha(), "INITIAL_ADMIN_PASSWORD");
    }

    private void exigir(String valor, String variavel) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                "INITIAL_ADMIN_ENABLED=true exige a variável "
                    + variavel + " preenchida");
        }
    }
}
